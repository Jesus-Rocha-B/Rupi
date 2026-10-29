from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.concurrency import run_in_threadpool
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, BeforeValidator, Field
from typing import Annotated, Optional, List, Dict, Any, Literal
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

# Límites de entrada: sin ellos, un texto o archivo enorme ocupaba memoria y gastaba la cuota de Gemini.
MAX_ARCHIVOS = 5
MAX_BYTES_ARCHIVO = 10 * 1024 * 1024
TIPOS_DE_PARADA = {"LECCION", "RETO", "CUESTIONARIO", "TARJETAS_MEMORIA", "INFOGRAFIA"}


def recortado(maximo: int):
    """Texto de contexto que se recorta en vez de rechazarse: una lección larga no debe romper el chat del niño."""
    return Annotated[Optional[str], BeforeValidator(lambda v: v[:maximo] if isinstance(v, str) else v)]


HISTORIAL_USADO = 10


class TextGenerateRequest(BaseModel):
    text: str = Field(max_length=60000)
    gradeNumber: Optional[int] = Field(default=None, ge=0, le=6)
    subjectCode: Optional[str] = Field(default=None, max_length=20)

class ChatContext(BaseModel):
    roadmapTitle: recortado(300) = "Ruta de Aprendizaje"
    roadmapDescription: recortado(3000) = ""
    stopsSummary: recortado(12000) = ""
    currentGrade: recortado(60) = ""
    currentSubject: recortado(120) = ""
    stopTitle: recortado(300) = "Parada Actual"
    lessonContent: recortado(20000) = ""

class ChatMessage(BaseModel):
    role: Literal["user", "assistant"]
    content: recortado(4000)

class ChatRequest(BaseModel):
    # Solo el mensaje del niño se rechaza si es absurdo; la web lo limita a 2000 caracteres al escribir.
    message: str = Field(max_length=2000)
    context: Optional[ChatContext] = None
    # La web guarda la conversación entera; se aceptan hasta 500 mensajes y se usan los últimos HISTORIAL_USADO.
    history: Optional[List[ChatMessage]] = Field(default_factory=list, max_length=500)


def _texto(valor, maximo):
    return valor.strip()[:maximo] if isinstance(valor, str) and valor.strip() else None


def validar_roadmap(crudo: str) -> Dict[str, Any]:
    """Revisa el JSON que devuelve Gemini antes de mandarlo al docente.

    Antes se devolvía tal cual: un texto que no era JSON daba 500 y una pregunta cuya respuesta correcta no estaba
    entre las opciones llegaba rota al editor. Lo indispensable (título y paradas) se exige; lo demás se ordena.
    """
    try:
        datos = json.loads(crudo)
    except (TypeError, ValueError) as error:
        raise IAError(f"Gemini no devolvió JSON válido: {crudo[:300]!r}") from error
    if not isinstance(datos, dict) or not _texto(datos.get("title"), 200):
        raise IAError("La ruta generada no tiene título.")
    paradas = datos.get("stops")
    if not isinstance(paradas, list) or not 1 <= len(paradas) <= 12:
        raise IAError("La ruta generada no tiene entre 1 y 12 paradas.")
    limpias = []
    for numero, parada in enumerate(paradas, start=1):
        if not isinstance(parada, dict) or not _texto(parada.get("title"), 200):
            raise IAError(f"La parada {numero} no tiene título.")
        parada["title"] = _texto(parada["title"], 200)
        parada["sequenceNumber"] = numero
        if parada.get("activityType") not in TIPOS_DE_PARADA:
            parada["activityType"] = "LECCION"
        opciones = parada.get("options")
        pregunta_valida = (isinstance(opciones, list) and 2 <= len(opciones) <= 6
                           and all(isinstance(o, str) and o.strip() for o in opciones)
                           and _texto(parada.get("challengeQuestion"), 500)
                           and parada.get("correctAnswer") in opciones)
        if not pregunta_valida:
            for campo in ("challengeQuestion", "options", "correctAnswer", "explanation"):
                parada.pop(campo, None)
        limpias.append(parada)
    datos["stops"] = limpias
    grado = datos.get("gradeNumber")
    if not isinstance(grado, int) or not 0 <= grado <= 6:
        datos["gradeNumber"] = None
    return datos

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
        if len(file_list) > MAX_ARCHIVOS:
            raise HTTPException(status_code=413, detail=f"Sube como máximo {MAX_ARCHIVOS} archivos a la vez.")
            
        extracted_sections = []
        filenames = []
        for f in file_list:
            content_bytes = await f.read(MAX_BYTES_ARCHIVO + 1)
            if len(content_bytes) > MAX_BYTES_ARCHIVO:
                raise HTTPException(status_code=413, detail=f"{f.filename} pesa más de 10 MB. Sube una versión más liviana.")
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
        
        roadmap_data = validar_roadmap(gemini_response)
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
        
        roadmap_data = validar_roadmap(gemini_response)
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
            for h in payload.history[-HISTORIAL_USADO:]:  # Mensajes recientes para memoria conversacional
                prefix = "Estudiante" if h.role == "user" else "Rupi"
                history_lines.append(f"{prefix}: {h.content or ''}")
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
