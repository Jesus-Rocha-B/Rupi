const fs = require('fs');
const crypto = require('crypto');
const { execSync } = require('child_process');

// Real, verified educational YouTube video IDs (Happy Learning, ChiquitinesTV, etc.)
const TOPIC_VIDEOS = {
  // Conteo / números
  conteo: { id: 'fVh43cTZzTE', title: 'Los Números del 1 al 100 - Conteo y Matemáticas' },
  // Suma
  suma: { id: 'oexd_Dfic_Q', title: 'Aprendiendo a sumar. La Suma | Happy Learning' },
  // Resta
  resta: { id: '42vjqtleG9E', title: 'Aprendiendo a restar. La Resta | Happy Learning' },
  // Monedas / Compras / Economía
  dinero: { id: 'VzBJ_FV6eoI', title: 'La historia del dinero y las monedas | Happy Learning' },
  // Patrones / Secuencias
  patron: { id: 'dzgnqza-2cU', title: 'Secuencias y patrones numéricos para primaria' },
  // Medición / Metro / Longitud
  longitud: { id: 'kzrplJ1jvko', title: 'La longitud y el metro | Happy Learning' },
  // Figuras y geometría
  geometria: { id: 'SXONzObzFk0', title: 'Las figuras geométricas planas | Vocabulario de Primaria' },
  // Reloj y tiempo
  reloj: { id: 'tBN6HGyuGwY', title: 'El reloj y las horas | Happy Learning' },
  // Cuentos / Fábulas / Comprensión lectora
  cuento: { id: 'K7Bvi6zMDWk', title: 'Cuento andino: El zorro y el cuy' },
  lectura: { id: 'pCyPzdzrvEI', title: 'Comprensión lectora y textos narrativos' },
  // Plantas / Seres vivos
  plantas: { id: 'ThbSPNlRz0o', title: 'El reino de las plantas y su clasificación | Happy Learning' },
  // Ecosistemas / Medio ambiente
  ecosistema: { id: 'SsRdLoYOMQA', title: 'Los Ecosistemas | Happy Learning' },
};

function getVideoForActivity(title) {
  const t = (title || '').toLowerCase();
  if (t.includes('sum') || t.includes('adici')) return TOPIC_VIDEOS.suma;
  if (t.includes('rest') || t.includes('sustra')) return TOPIC_VIDEOS.resta;
  if (t.includes('moneda') || t.includes('compra') || t.includes('mercado') || t.includes('dinero') || t.includes('presupuesto')) return TOPIC_VIDEOS.dinero;
  if (t.includes('patr') || t.includes('secuencia') || t.includes('quipu')) return TOPIC_VIDEOS.patron;
  if (t.includes('med') || t.includes('metro') || t.includes('longitud') || t.includes('perímetro') || t.includes('perimetro') || t.includes('balanza')) return TOPIC_VIDEOS.longitud;
  if (t.includes('forma') || t.includes('figura') || t.includes('geom') || t.includes('ángulo') || t.includes('triángulo') || t.includes('prisma')) return TOPIC_VIDEOS.geometria;
  if (t.includes('hora') || t.includes('reloj') || t.includes('campana') || t.includes('tiempo')) return TOPIC_VIDEOS.reloj;
  if (t.includes('zorro') || t.includes('cuy') || t.includes('fábula') || t.includes('fabula') || t.includes('leyenda') || t.includes('mito') || t.includes('relato')) return TOPIC_VIDEOS.cuento;
  if (t.includes('planta') || t.includes('flora') || t.includes('biohuerto') || t.includes('raíz') || t.includes('semilla')) return TOPIC_VIDEOS.plantas;
  if (t.includes('ecosistema') || t.includes('agua') || t.includes('río') || t.includes('ambiente') || t.includes('solar') || t.includes('energía') || t.includes('célula')) return TOPIC_VIDEOS.ecosistema;
  if (t.includes('vocal') || t.includes('rima') || t.includes('palabra') || t.includes('pregón') || t.includes('texto') || t.includes('noticia') || t.includes('afiche') || t.includes('ensayo') || t.includes('discurso')) return TOPIC_VIDEOS.lectura;
  if (t.includes('cont') || t.includes('número') || t.includes('numero') || t.includes('1 al') || t.includes('plaza') || t.includes('paso')) return TOPIC_VIDEOS.conteo;
  return TOPIC_VIDEOS.suma;
}

// 1. Fetch activities from MySQL
const rawOutput = execSync(
  '& "C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysql.exe" -u rupi -prochabobadilla rupi --batch --raw -e "SELECT id, titulo FROM aprendizaje_version_actividad;"',
  { shell: 'powershell.exe' }
).toString('utf8');

const lines = rawOutput.trim().split('\n').slice(1); // skip header
console.log(`Processing ${lines.length} activities...`);

const statements = [
  'START TRANSACTION;',
  '-- Eliminar videos previos para recrear limpiamente con videos reales de YouTube',
  "DELETE FROM aprendizaje_bloque_contenido WHERE tipo = 'VIDEO';",
  "DELETE FROM recurso_archivo_multimedia WHERE proveedor_almacenamiento = 'YOUTUBE';"
];

for (const line of lines) {
  const parts = line.split('\t');
  if (parts.length < 2) continue;
  const id = parts[0].trim();
  const title = parts[1].trim();
  const video = getVideoForActivity(title);
  const mediaId = crypto.randomUUID();
  const blockId = crypto.randomUUID();
  const youtubeUrl = `https://www.youtube.com/watch?v=${video.id}`;
  const escTitle = video.title.replace(/'/g, "''");
  const escActTitle = title.replace(/'/g, "''");

  statements.push(`
INSERT INTO recurso_archivo_multimedia
  (id, proveedor_almacenamiento, url_externa, nombre_original, tipo_mime, texto_accesibilidad)
VALUES
  ('${mediaId}', 'YOUTUBE', '${youtubeUrl}', '${escTitle}', 'video/mp4', 'Video didáctico de YouTube para ${escActTitle}');

INSERT INTO aprendizaje_bloque_contenido
  (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo, archivo_multimedia_id, texto_accesibilidad)
VALUES
  ('${blockId}', '${id}', 50, 'VIDEO', '${escTitle}', '${mediaId}', 'Video didáctico: ${escTitle}');
`);
}

statements.push('COMMIT;');

const sqlContent = statements.join('\n');
fs.writeFileSync('scripts/seed-youtube-videos.sql', sqlContent, 'utf8');
console.log('Generated scripts/seed-youtube-videos.sql with', lines.length, 'video blocks.');

// Execute SQL
execSync(
  'Get-Content scripts/seed-youtube-videos.sql -Raw | & "C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysql.exe" -u rupi -prochabobadilla rupi',
  { shell: 'powershell.exe' }
);
console.log('Database successfully seeded with real YouTube videos!');
