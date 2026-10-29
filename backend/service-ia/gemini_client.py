import os
import re
import json
import urllib.request
import urllib.error
from pathlib import Path


class IANoConfigurada(RuntimeError):
    """No hay GEMINI_API_KEY: el servicio responde 503 sin intentar llamar a Gemini."""


class IAError(RuntimeError):
    """Gemini falló o respondió algo inútil. El detalle es para el log del servidor, nunca para el cliente."""


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
        raise IANoConfigurada("No se ha configurado GEMINI_API_KEY. Agrégala en .local/env.ps1")
        
    # La clave va en la cabecera x-goog-api-key: en la URL quedaba en logs, proxies y mensajes de error.
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
    
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
        
    headers = {"Content-Type": "application/json", "x-goog-api-key": api_key}
    body_bytes = json.dumps(payload).encode("utf-8")
    
    req = urllib.request.Request(url, data=body_bytes, headers=headers, method="POST")
    
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            candidates = data.get("candidates", [])
            if not candidates:
                raise IAError("Gemini no devolvió candidatos en la respuesta.")
            return candidates[0]["content"]["parts"][0]["text"]

    except IAError:
        raise
    except urllib.error.HTTPError as e:
        error_msg = e.read().decode("utf-8", errors="ignore")[:2000]
        raise IAError(f"Error de Gemini ({e.code}): {error_msg}") from e
    except Exception as e:
        raise IAError(f"Error al conectar con Gemini: {e}") from e
