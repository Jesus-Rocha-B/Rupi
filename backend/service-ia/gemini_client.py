import os
import re
import json
import urllib.request
import urllib.error
from pathlib import Path

def get_api_key() -> str:
    """Obtiene la API key de Gemini desde variables de entorno o desde .local/env.ps1."""
    key = os.getenv("GEMINI_API_KEY") or os.getenv("RUPI_AI_KEY")
    if key and key.strip():
        return key.strip()
        
    # Buscar en .local/env.ps1 si no está en os.environ
    root_dir = Path(__file__).resolve().parent.parent.parent
    env_file = root_dir / ".local" / "env.ps1"
    if env_file.exists():
        try:
            content = env_file.read_text(encoding="utf-8")
            match = re.search(r"['\"]GEMINI_API_KEY['\"]\s*=\s*['\"]([^'\"]+)['\"]", content)
            if not match:
                match = re.search(r"\$env:GEMINI_API_KEY\s*=\s*['\"]([^'\"]+)['\"]", content)
            if match:
                return match.group(1).strip()
        except Exception:
            pass
            
    return ""

def call_gemini(system_instruction: str, user_content: str, json_mode: bool = False, model: str = "gemini-flash-lite-latest") -> str:
    """Llama a la API de Google Gemini en su capa gratuita."""
    api_key = get_api_key()
    if not api_key:
        raise ValueError("No se ha configurado GEMINI_API_KEY. Agrégala en .local/env.ps1")
        
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
    
    payload = {
        "contents": [
            {
                "parts": [{"text": user_content}]
            }
        ]
    }
    
    if system_instruction:
        payload["system_instruction"] = {
            "parts": [{"text": system_instruction}]
        }
        
    if json_mode:
        payload["generationConfig"] = {
            "response_mime_type": "application/json"
        }
        
    headers = {"Content-Type": "application/json"}
    body_bytes = json.dumps(payload).encode("utf-8")
    
    req = urllib.request.Request(url, data=body_bytes, headers=headers, method="POST")
    
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            candidates = data.get("candidates", [])
            if not candidates:
                raise ValueError("Gemini no devolvió candidatos en la respuesta.")
            return candidates[0]["content"]["parts"][0]["text"]
            
    except urllib.error.HTTPError as e:
        error_msg = e.read().decode("utf-8", errors="ignore")
        raise RuntimeError(f"Error de Gemini ({e.code}): {error_msg}")
    except Exception as e:
        raise RuntimeError(f"Error al conectar con Gemini: {str(e)}")
