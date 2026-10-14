"""Verifica HU-12 contra API local: biblioteca, grado/materia, ZIP y hashes.
Uso: python scripts/verify_hu12_resources.py --api http://127.0.0.1:8081
Solo consulta rutas inscritas y crea una sesión de la cuenta demo; no completa lecciones.
"""
import argparse, hashlib, http.cookiejar, io, json, urllib.request, urllib.error, urllib.parse, zipfile

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--api',default='http://127.0.0.1:8081')
    args=parser.parse_args()
    parsed=urllib.parse.urlparse(args.api)
    if parsed.scheme!='http' or parsed.hostname not in ('localhost','127.0.0.1'):
        parser.error('Usa solo la API local de desarrollo.')
    api=args.api.rstrip('/')+'/api/v1/'
    client=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    def request(path,data=None):
        req=urllib.request.Request(api+path,data=None if data is None else json.dumps(data).encode(),headers={'Content-Type':'application/json','Origin':'http://127.0.0.1:5173'})
        return client.open(req,timeout=60)
    def get(path):
        with request(path) as response: return json.load(response)
    with request('auth/login',{'username':'estudiante.demo','password':'123456'}): pass
    routes=get('student/learning-routes')
    if isinstance(routes,dict): routes=routes.get('items',[])
    aliases={'MAT':'MATEMATICA','MATEMATICA':'MATEMATICA','COM':'COMUNICACION','COMUNICACION':'COMUNICACION','CYT':'CYT','CT':'CYT','CIENCIA':'CYT','CIENCIA_TECNOLOGIA':'CYT'}
    covered=set()
    for route in routes:
        area=aliases.get(route['area']['code'].upper());grade=route['grade']['number']
        if not area or (grade,area) in covered: continue
        base='student/learning-routes/'+route['versionRouteId']
        sources=get(base+'/study-resources')
        assert len(sources)==1 and sources[0]['grade']==grade and sources[0]['areaCode']==area
        detail=get(base);units=detail['curriculum']['units']
        unit=next(u for u in units if u.get('totalNodes',0)>0 or u.get('nodes') or u.get('nodeIds'))
        with request(base+'/units/'+(unit.get('id') or 'otras')+'/package') as response:
            content=response.read();assert hashlib.sha256(content).hexdigest()==response.headers['X-Rupi-Paquete-Sha256']
        with zipfile.ZipFile(io.BytesIO(content)) as pack:
            assert pack.read('repaso-rupi.pdf').startswith(b'%PDF-')
            assert sources[0]['sourceUrl'] in pack.read('fuentes-minedu.html').decode()
            unit_content=json.loads(pack.read('contenido.json'))['unidad']
            assert unit_content['supplementaryResources']==sources
            manifest=json.loads(pack.read('manifest.json'))
            for part in manifest['archivos']:
                raw=pack.read(part['ruta']);assert len(raw)==part['bytes'];assert hashlib.sha256(raw).hexdigest()==part['sha256']
        node=detail['nodes'][0]
        with request(base+'/nodes/'+node['id']+'/package') as response:
            content=response.read();assert hashlib.sha256(content).hexdigest()==response.headers['X-Rupi-Paquete-Sha256']
        with zipfile.ZipFile(io.BytesIO(content)) as pack:
            stops=json.loads(pack.read('contenido.json'))['unidad']['stops']
            assert len(stops)==1 and stops[0]['sequence']==node['sequence']
            assert 'MI MOCHILA SIN CONEXIÓN' in pack.read('ABRIR.html').decode()
        try: get(base+'/nodes/00000000-0000-4000-8000-000000000000/package')
        except urllib.error.HTTPError as error: assert error.code==404
        else: raise AssertionError('Se permitió un nodo ajeno')
        covered.add((grade,area));print(f'OK {grade} {area}: fuente, grado, repaso y hashes')
    assert covered,'No hay rutas inscritas de primaria para verificar.'
    # Rechazo real de sesión ausente y ruta ajena para el nuevo endpoint.
    try: urllib.request.urlopen(api+'student/learning-routes/'+routes[0]['versionRouteId']+'/study-resources')
    except urllib.error.HTTPError as error: assert error.code==401
    else: raise AssertionError('Se permitió consultar sin sesión')
    try: get('student/learning-routes/00000000-0000-4000-8000-000000000000/study-resources')
    except urllib.error.HTTPError as error: assert error.code==404
    else: raise AssertionError('Se permitió consultar una ruta ajena')
    print(f'{len(covered)} combinaciones verificadas; accesos 401 y 404 correctos.')

if __name__=='__main__': main()
