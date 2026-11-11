import json
import os
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from unittest.mock import patch
from moderation_client import check_response, ModerationUnavailable
import gemini_client


class ModerationTest(unittest.TestCase):
    def test_real_http_fail_closed(self):
        state={'status':200,'body':{'permitida':True},'token':None}
        class Handler(BaseHTTPRequestHandler):
            def do_POST(self):
                state['token']=self.headers.get('X-Rupi-Monitor-Token')
                state['request']=json.loads(self.rfile.read(int(self.headers['Content-Length'])))
                self.send_response(state['status']);self.end_headers();self.wfile.write(json.dumps(state['body']).encode())
            def log_message(self,*args): pass
        server=ThreadingHTTPServer(('127.0.0.1',0),Handler)
        thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
        try:
            with patch.dict(os.environ,{'RUPI_ADMIN_URL':f'http://127.0.0.1:{server.server_port}','RUPI_MONITOR_TOKEN':'fixture-token'}):
                self.assertTrue(check_response('Texto', {'modelo':'fixture','duracionMs':45})['permitida']);self.assertEqual(state['token'],'fixture-token')
                self.assertEqual(state['request']['auditoria']['modelo'],'fixture')
                self.assertEqual(state['request']['auditoria']['duracionMs'],45)
                for invalid in [{},{'permitida':'true'},{'permitida':False}]:
                    state['body']=invalid
                    with self.assertRaises(ModerationUnavailable):check_response('Texto')
                state.update(status=503,body={'permitida':True})
                with self.assertRaises(ModerationUnavailable):check_response('Texto')
        finally:server.shutdown();server.server_close();thread.join()

    def test_generated_output_is_replaced_or_rejected(self):
        from test_servicio_ia import _Respuesta
        payload=json.dumps({'candidates':[{'content':{'parts':[{'text':'No sabes nada'}]}}]}).encode()
        with patch.dict(os.environ,{'GEMINI_API_KEY':'fixture'}),patch('urllib.request.urlopen',return_value=_Respuesta(payload)),patch('gemini_client.check_response',return_value={'permitida':False,'alternativa':'Consulta a tu docente.'}):
            self.assertEqual(gemini_client.call_gemini('system','text'),'Consulta a tu docente.')
            with self.assertRaises(gemini_client.IAError):gemini_client.call_gemini('system','text',json_mode=True)

    def test_unavailable_moderation_never_returns_original(self):
        from test_servicio_ia import _Respuesta
        payload=json.dumps({'candidates':[{'content':{'parts':[{'text':'Texto sin revisar'}]}}]}).encode()
        with patch.dict(os.environ,{'GEMINI_API_KEY':'fixture'}),patch('urllib.request.urlopen',return_value=_Respuesta(payload)),patch('gemini_client.check_response',side_effect=ModerationUnavailable('No disponible')):
            with self.assertRaises(gemini_client.IAError):gemini_client.call_gemini('system','text')
