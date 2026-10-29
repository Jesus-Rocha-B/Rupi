from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.concurrency import run_in_threadpool
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
import json
import logging
import os

from extractor import extract_text_from_file
from gemini_client import IAError, IANoConfigurada, call_gemini, get_api_key
from prompts import SYSTEM_ROADMAP_GENERATOR, SYSTEM_RUPI_TUTOR, level_for_grade

log = logging.getLogger("rupi.ia")


def _fallo(error: Exception, accion: str) -> HTTPException:
    """El detalle queda en el log del servidor; el cliente recibe un mensaje claro, sin trazas ni texto de Gemini."""
    if isinstance(error, IANoConfigurada):
        return HTTPException(status_code=503, detail="La IA todavía no está configurada en este servidor.")
    log.exception("Fallo al %s", accion)
    if isinstance(error, IAError):
        return HTTPException(status_code=502, detail="El servicio de IA no respondió bien. Intenta de nuevo en un momento.")
    return HTTPException(status_code=500, detail=f"No pudimos {accion}. Intenta de nuevo en un momento.")


app = FastAPI(
    title="RUPI - Servicio de IA Educativa",
    description="Microservicio de IA estilo NotebookLM para generación de Roadmaps y Tutor socrático Rupi.",
    version="1.0.0"
)

# La web llega por el proxy de Vite (mismo origen) y Spring llama de servidor a servidor, así que CORS solo
# admite los orígenes de la web. «*» con credenciales permitía que cualquier página usara la IA con la sesión ajena.
ORIGENES = [o.strip() for o in os.getenv("RUPI_IA_ORIGINS", "http://localhost:5173,http://127.0.0.1:5173").split(",")
            if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=ORIGENES,
    allow_credentials=False,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type"],
)

class TextGenerateRequest(BaseModel):
    text: str
    gradeNumber: Optional[int] = None
    subjectCode: Optional[str] = None

class ChatContext(BaseModel):
    roadmapTitle: Optional[str] = "Ruta de Aprendizaje"
    roadmapDescription: Optional[str] = ""
    stopsSummary: Optional[str] = ""
    currentGrade: Optional[str] = ""
    currentSubject: Optional[str] = ""
    stopTitle: Optional[str] = "Parada Actual"
    lessonContent: Optional[str] = ""

class ChatMessage(BaseModel):
    role: str  # 'user' | 'assistant'
    content: str

class ChatRequest(BaseModel):
    message: str
    context: Optional[ChatContext] = None
    history: Optional[List[ChatMessage]] = []

@app.get("/health")
@app.get("/api/v1/ia/health")
def health_check():
    key = get_api_key()
    return {
        "status": "online",
        "service": "rupi-ia-notebooklm",
        "geminiConfigured": bool(key),
        "model": "gemini-flash-lite-latest"
    }

@app.post("/api/v1/ia/roadmap/generate-from-file")
async def generate_roadmap_from_file(
    files: Optional[List[UploadFile]] = File(None),
    file: Optional[UploadFile] = File(None),
    gradeNumber: Optional[int] = Form(None),
    subjectCode: Optional[str] = Form(None)
):
    """Extrae el contenido de uno o múltiples archivos (PDF, DOCX, TXT) y genera un Roadmap estructurado."""
    try:
        file_list = []
        if files:
            file_list.extend(files)
        if file and file not in file_list:
            file_list.append(file)
            
        if not file_list:
            raise HTTPException(status_code=400, detail="Debes subir al menos un archivo educativo.")
            
        extracted_sections = []
        filenames = []
        for f in file_list:
            content_bytes = await f.read()
            if not content_bytes:
                continue
            # Leer PDF o Word y llamar a Gemini bloquea: en un endpoint async congelaba todo el servicio, también el
            # tutor de otros estudiantes. Ese trabajo va a un hilo aparte.
            try:
                text = await run_in_threadpool(extract_text_from_file, content_bytes, f.filename)
            except ValueError as e:
                raise HTTPException(status_code=400, detail=str(e))
            except Exception:
                log.warning("No se pudo leer %s", f.filename, exc_info=True)
                raise HTTPException(status_code=400, detail=f"No pudimos leer {f.filename}. Revisa que no esté dañado.")
            if text.strip():
                filenames.append(f.filename)
                extracted_sections.append(f"=== DOCUMENTO: {f.filename} ===\n{text.strip()}")
                
        if not extracted_sections:
            raise HTTPException(status_code=400, detail="No se pudo extraer texto legible de los archivos subidos.")
            
        combined_text = "\n\n".join(extracted_sections)[:60000]
        
        user_prompt = f"MATERIALES EDUCATIVOS DE BASE (Archivos analizados: {', '.join(filenames)}):\n\n{combined_text}\n\n"
        if gradeNumber is not None:
            if gradeNumber == 0:
                user_prompt += "PREFERENCIA: Diseña una ruta de aprendizaje adaptada para primaria multigrado o sin grado fijo (libre).\n"
            else:
                user_prompt += f"PREFERENCIA: Diseña específicamente para {gradeNumber}.° de primaria.\n"
        if subjectCode:
            user_prompt += f"PREFERENCIA: Área curricular {subjectCode}.\n"
        user_prompt += "Genera el JSON completo de la ruta de aprendizaje siguiendo el esquema estricto."
        
        gemini_response = await run_in_threadpool(
            call_gemini,
            system_instruction=SYSTEM_ROADMAP_GENERATOR,
            user_content=user_prompt,
            json_mode=True
        )
        
        roadmap_data = json.loads(gemini_response)
        # Asegurar que cada parada indique obligatoriamente sus documentos base
        if "stops" in roadmap_data and isinstance(roadmap_data["stops"], list):
            for s in roadmap_data["stops"]:
                if not s.get("sourceDocuments") or not isinstance(s["sourceDocuments"], list) or len(s["sourceDocuments"]) == 0:
                    s["sourceDocuments"] = list(filenames)

        return {
            "success": True,
            "filenames": filenames,
            "roadmap": roadmap_data
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise _fallo(e, "generar el roadmap")

@app.post("/api/v1/ia/roadmap/generate-from-text")
def generate_roadmap_from_text(payload: TextGenerateRequest):
    """Genera un Roadmap estructurado a partir de texto o apuntes curriculares pegados por el docente."""
    try:
        if not payload.text.strip():
            raise HTTPException(status_code=400, detail="El texto está vacío.")
            
        trimmed_text = payload.text[:50000]
        user_prompt = f"MATERIAL EDUCATIVO BASE:\n\n{trimmed_text}\n\n"
        if payload.gradeNumber:
            user_prompt += f"PREFERENCIA: Diseña para {payload.gradeNumber}.° de primaria.\n"
        if payload.subjectCode:
            user_prompt += f"PREFERENCIA: Área curricular {payload.subjectCode}.\n"
        user_prompt += "Genera el JSON completo de la ruta de aprendizaje siguiendo el esquema estricto."
        
        gemini_response = call_gemini(
            system_instruction=SYSTEM_ROADMAP_GENERATOR,
            user_content=user_prompt,
            json_mode=True
        )
        
        roadmap_data = json.loads(gemini_response)
        return {
            "success": True,
            "roadmap": roadmap_data
        }
    except HTTPException:
        raise
    except Exception as e:
        raise _fallo(e, "generar el roadmap")

@app.post("/api/v1/ia/tutor/chat")
def tutor_chat(payload: ChatRequest):
    """Chat interactivo con Rupi fundamentado exclusivamente en el contenido de la parada o Roadmap (NotebookLM)."""
    try:
        if not payload.message.strip():
            raise HTTPException(status_code=400, detail="El mensaje está vacío.")
            
        ctx = payload.context or ChatContext()
        context_block = f"""
DOCUMENTO Y CONTEXTO COMPLETO DEL ROADMAP (GROUNDING NOTEBOOKLM):
- Título del Roadmap Escolar: {ctx.roadmapTitle}
- Descripción General de la Ruta: {ctx.roadmapDescription if ctx.roadmapDescription else "Aventura escolar de aprendizaje por etapas."}
- Materia y Grado: {ctx.currentSubject} {ctx.currentGrade}
- NIVEL DEL ESTUDIANTE: {level_for_grade(ctx.currentGrade)}
- Paradas Didácticas que componen este Roadmap:
{ctx.stopsSummary if ctx.stopsSummary else "Secuencia didáctica de paradas y lecciones interactivas."}

PARADA DONDE ESTÁ EL ESTUDIANTE AHORA:
- Parada actual: {ctx.stopTitle}
- Contenido de la parada activa:
{ctx.lessonContent if ctx.lessonContent else "El estudiante explora actividades didácticas en esta parada."}
"""
        # Incluir historial de conversación para continuidad NotebookLM
        history_text = ""
        if payload.history:
            history_lines = []
            for h in payload.history[-10:]:  # Hasta 10 intercambios recientes para memoria conversacional
                prefix = "Estudiante" if h.role == "user" else "Rupi"
                history_lines.append(f"{prefix}: {h.content}")
            history_text = "\nHISTORIAL DE CONVERSACIÓN PREVIO:\n" + "\n".join(history_lines) + "\n"

        user_content = f"{context_block}\n{history_text}\nPREGUNTA ACTUAL DEL ESTUDIANTE:\n{payload.message}"
        
        reply = call_gemini(
            system_instruction=SYSTEM_RUPI_TUTOR,
            user_content=user_content,
            json_mode=False
        )
        
        return {
            "reply": reply,
            "role": "assistant"
        }
        
    except HTTPException:
        raise
    except Exception as e:
        raise _fallo(e, "responder en el tutor Rupi")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)
