"""Filtro de patrones; no sustituye revisión pedagógica ni moderación semántica."""
import json
import re
import unicodedata
from identidad.respuestas import ErrorDePeticion

REGLAS = [
    {'codigo': 'RESPETO', 'nombre': 'Lenguaje respetuoso', 'descripcion': 'Evitar insultos y humillaciones'},
    {'codigo': 'MENORES', 'nombre': 'Contenido adecuado para menores', 'descripcion': 'Evitar contenido sexual explícito y violencia gráfica'},
    {'codigo': 'PRIVACIDAD', 'nombre': 'Privacidad del estudiante', 'descripcion': 'No pedir contraseñas ni datos personales'},
    {'codigo': 'SEGURIDAD', 'nombre': 'Orientación educativa segura', 'descripcion': 'No promover conductas peligrosas'},
]
ALTERNATIVAS = ['Vamos a explicarlo de otra manera, con respeto y palabras claras.',
    'Inténtalo otra vez. Te doy una pista para ayudarte.',
    'Consulta este tema con tu docente o con un adulto de confianza.']
PATRONES = {
    'RESPETO': [r'\b(?:idiota|imbecil|estupido|estupida|inutil|tonto|tonta)\b', r'\bno sabes nada\b', r'\bno sirves\b'],
    'MENORES': [r'\b(?:pornografia|pornografico|pornografica|sexo explicito)\b', r'\b(?:descuartiza|decapita|mutila)\w*\b'],
    'PRIVACIDAD': [r'\b(?:dime|envia|enviame|comparte|dame|escribe|manda|mandame|indica|indicame)\b.{0,65}\b(?:contrasena|clave|direccion|telefono|dni|correo|foto)\b', r'[\w.+-]+@[\w.-]+\.[a-z]{2,}', r'\b\d{8,}\b'],
    'SEGURIDAD': [r'\b(?:hazte dano|lastimate|suicidate|mata a|golpea a|cortate|bebe lejia|toma veneno)\b', r'\b(?:fabrica|construye|prepara)\b.{0,30}\b(?:bomba|explosivo)\b'],
}
CASOS = [
    ('Respuesta respetuosa', 'Inténtalo otra vez. Puedes contar con tus dedos.', False),
    ('Lenguaje humillante', 'No sabes nada. Esa respuesta es tonta.', True),
    ('Datos personales', 'Dime tu contraseña para ayudarte.', True),
    ('Contenido adulto', 'Busca pornografía para esta tarea.', True),
    ('Conducta peligrosa', 'Bebe lejía para completar el reto.', True),
    ('Palabra dentro de otra', 'La tonalidad del dibujo es cálida.', False),
]


def normalizar(texto):
    texto = unicodedata.normalize('NFKD', texto.casefold())
    texto = ''.join(c for c in texto if unicodedata.category(c) not in ('Mn', 'Cf'))
    return re.sub(r'\s+', ' ', texto).strip()


def inicial():
    return {'extras': {r['codigo']: [] for r in REGLAS}, 'alternativa': ALTERNATIVAS[0]}


def validar(datos):
    if not isinstance(datos, dict) or set(datos) != {'extras', 'alternativa'}:
        raise ErrorDePeticion('La configuración debe contener frases adicionales y mensaje alternativo.')
    extras = datos['extras']
    if not isinstance(extras, dict) or set(extras) != set(PATRONES):
        raise ErrorDePeticion('Conserva las cuatro reglas obligatorias de protección.')
    for terms in extras.values():
        if not isinstance(terms, list) or len(terms) > 20:
            raise ErrorDePeticion('Usa hasta 20 frases por regla.')
        if any(not isinstance(t, str) or not 3 <= len(t.strip()) <= 80 for t in terms):
            raise ErrorDePeticion('Cada frase debe tener entre 3 y 80 caracteres.')
        if any(not normalizar(t).replace(' ', '').isalnum() for t in terms):
            raise ErrorDePeticion('Usa frases con letras, números y espacios; no expresiones regulares.')
    if datos['alternativa'] not in ALTERNATIVAS:
        raise ErrorDePeticion('Selecciona uno de los mensajes alternativos revisados.')
    config = {'extras': {k: sorted(set(normalizar(t) for t in v)) for k, v in extras.items()}, 'alternativa': datos['alternativa']}
    if not evaluar(config['alternativa'], config)['permitida']:
        raise ErrorDePeticion('Las frases añadidas bloquean el mensaje alternativo. Elige otro mensaje o revisa las frases.')
    return config


def contenido(texto):
    try:
        parsed = json.loads(texto)
        def strings(obj):
            if isinstance(obj, str): return [obj]
            if isinstance(obj, list): return [s for x in obj for s in strings(x)]
            if isinstance(obj, dict): return [s for x in obj.values() for s in strings(x)]
            return []
        return '\n'.join(strings(parsed)) or texto
    except (ValueError, RecursionError):
        return texto


def evaluar(texto, config):
    if not isinstance(texto, str) or not texto.strip() or len(texto) > 30000:
        raise ErrorDePeticion('Escribe una respuesta de entre 1 y 30000 caracteres.')
    normalized = normalizar(contenido(texto))
    matches = []
    for rule in REGLAS:
        code = rule['codigo']
        patterns = PATRONES[code] + [r'(?<!\w)' + re.escape(t) + r'(?!\w)' for t in config['extras'][code]]
        if any(re.search(p, normalized) for p in patterns): matches.append(rule['nombre'])
    return {'permitida': not matches, 'reglas': matches, 'alternativa': config['alternativa'] if matches else None,
            'metodo': 'PATRONES', 'aviso': 'Sin coincidencias no significa seguridad garantizada. Mantén la revisión docente.'}


def bateria(config):
    return [{'caso': name, 'correcto': (not evaluar(text, config)['permitida']) == blocked} for name, text, blocked in CASOS]
