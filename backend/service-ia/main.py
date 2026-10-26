from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
import json
import traceback

from extractor import extract_text_from_file
from gemini_client import call_gemini, get_api_key
from prompts import SYSTEM_ROADMAP_GENERATOR, SYSTEM_RUPI_TUTOR

app = FastAPI(
    title="RUPI - Servicio de IA Educativa",
    description="Microservicio de IA estilo NotebookLM para generación de Roadmaps y Tutor socrático Rupi.",
    version="1.0.0"
)

# Habilitar CORS para interactuar tanto con Vite (5173) como con Spring Boot (8081)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class TextGenerateRequest(BaseModel):
    text: str
    gradeNumber: Optional[int] = None
    subjectCode: Optional[str] = None

class ChatContext(BaseModel):
    roadmapTitle: Optional[str] = "Ruta de Aprendizaje"
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
    file: UploadFile = File(...),
    gradeNumber: Optional[int] = Form(None),
    subjectCode: Optional[str] = Form(None)
):
    """Extrae el contenido de un archivo (PDF, DOCX, TXT) y genera un Roadmap estructurado."""
    try:
        content_bytes = await file.read()
        if not content_bytes:
            raise HTTPException(status_code=400, detail="El archivo subido está vacío.")
            
        extracted_text = extract_text_from_file(content_bytes, file.filename)
        if not extracted_text.strip():
            raise HTTPException(status_code=400, detail="No se pudo extraer texto legible del archivo.")
            
        # Limitar longitud para optimizar tokens si es un libro muy largo (máximo ~50,000 caracteres)
        trimmed_text = extracted_text[:50000]
        
        user_prompt = f"MATERIAL EDUCATIVO BASE (Nombre de archivo: {file.filename}):\n\n{trimmed_text}\n\n"
        if gradeNumber:
            user_prompt += f"PREFERENCIA: Diseña específicamente para {gradeNumber}.° de primaria.\n"
        if subjectCode:
            user_prompt += f"PREFERENCIA: Área curricular {subjectCode}.\n"
        user_prompt += "Genera el JSON completo de la ruta de aprendizaje siguiendo el esquema estricto."
        
        gemini_response = call_gemini(
            system_instruction=SYSTEM_ROADMAP_GENERATOR,
            user_content=user_prompt,
            json_mode=True
        )
        
        roadmap_data = json.loads(gemini_response)
        return {
            "success": True,
            "filename": file.filename,
            "roadmap": roadmap_data
        }
        
    except HTTPException:
        raise
    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Error al generar roadmap: {str(e)}")

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
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Error al generar roadmap: {str(e)}")

@app.post("/api/v1/ia/tutor/chat")
def tutor_chat(payload: ChatRequest):
    """Chat interactivo con Rupi fundamentado exclusivamente en el contenido de la parada o Roadmap (NotebookLM)."""
    try:
        if not payload.message.strip():
            raise HTTPException(status_code=400, detail="El mensaje está vacío.")
            
        ctx = payload.context or ChatContext()
        context_block = f"""
DOCUMENTO Y CONTEXTO DE LA LECCIÓN (GROUNDING NOTEBOOKLM):
- Ruta de Aprendizaje: {ctx.roadmapTitle}
- Parada / Actividad Actual: {ctx.stopTitle}
- Contenido de Estudio:
{ctx.lessonContent if ctx.lessonContent else "En esta parada los estudiantes aprenden nociones y habilidades prácticas de la lección."}
"""
        # Incluir historial breve si existe
        history_text = ""
        if payload.history:
            history_lines = []
            for h in payload.history[-4:]:  # Últimos 4 mensajes
                prefix = "Estudiante" if h.role == "user" else "Rupi"
                history_lines.append(f"{prefix}: {h.content}")
            history_text = "\nHISTORIAL RECIENTE:\n" + "\n".join(history_lines) + "\n"

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
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Error en el tutor Rupi: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)
