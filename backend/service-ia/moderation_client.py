"""Cliente interno HU-53. No conserva respuestas ni claves en logs."""
import json
import os
import urllib.request
import urllib.error
from urllib.parse import urlparse


class ModerationUnavailable(RuntimeError):
    pass


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def check_response(text, audit=None):
    base = os.environ.get('RUPI_ADMIN_URL', 'http://127.0.0.1:8182').strip().rstrip('/')
    token = os.environ.get('RUPI_MONITOR_TOKEN', '').strip()
    uri = urlparse(base)
    if not token or not (uri.scheme == 'https' or (uri.scheme == 'http' and uri.hostname in ('127.0.0.1', 'localhost', '::1'))):
        raise ModerationUnavailable('Moderación no configurada.')
    request = urllib.request.Request(base + '/api/internal/moderacion/evaluar',
        data=json.dumps({'texto': text, 'auditoria': audit or {}}, ensure_ascii=False).encode('utf-8'),
        headers={'Content-Type': 'application/json', 'X-Rupi-Monitor-Token': token}, method='POST')
    try:
        with urllib.request.build_opener(NoRedirect).open(request, timeout=4) as response:
            payload = response.read(8001)
            if len(payload) > 8000: raise ValueError('Respuesta excesiva')
            decision = json.loads(payload)
            if not isinstance(decision.get('permitida'), bool): raise ValueError('Decisión inválida')
            if not decision['permitida'] and not isinstance(decision.get('alternativa'), str): raise ValueError('Alternativa inválida')
            return decision
    except urllib.error.HTTPError as error:
        error.close()
        raise ModerationUnavailable('No se pudo verificar la respuesta de IA.') from None
    except Exception:
        raise ModerationUnavailable('No se pudo verificar la respuesta de IA.') from None
