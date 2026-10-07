// HU-04: abre el mapa en el navegador y compara lo que se DIBUJA (orden de bandas y paradas, de arriba abajo)
// con scripts/fixtures/hu04-orden-esperado.json. Solo entorno local, con Rupi levantado (./iniciar-rupi.sh).
// Playwright no es dependencia del repo. Instálalo fuera de él y pasa su carpeta:
//   mkdir -p /tmp/pw && cd /tmp/pw && npm init -y && npm i playwright && npx playwright install firefox
//   PLAYWRIGHT_DIR=/tmp/pw node scripts/verify_hu04_pantalla.mjs [http://localhost:5173]
import { createRequire } from 'node:module'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const dir = process.env.PLAYWRIGHT_DIR
if (!dir) { console.error('Define PLAYWRIGHT_DIR (ver el encabezado del archivo).'); process.exit(2) }
const { firefox } = createRequire(join(dir, 'package.json'))('playwright')
const base = process.argv[2] ?? 'http://localhost:5173'
if (!/^http:\/\/(localhost|127\.0\.0\.1):/.test(base)) { console.error('Solo contra la web local.'); process.exit(2) }
const fx = JSON.parse(readFileSync(join(dirname(fileURLToPath(import.meta.url)), 'fixtures', 'hu04-orden-esperado.json'), 'utf8'))
const esperado = fx.unidades.map(u => ({ titulo: u.titulo, paradas: u.paradas }))
let fallas = 0
const check = (nombre, ok, detalle = '') => { console.log(ok ? 'OK:' : 'FALLA:', nombre, ok ? '' : detalle); if (!ok) fallas++ }

const browser = await firefox.launch()
for (const [nombre, w, h] of [['escritorio 1280 px', 1280, 900], ['móvil 375 px', 375, 800]]) {
  const ctx = await browser.newContext({ viewport: { width: w, height: h } })
  const page = await ctx.newPage()
  await ctx.request.post(`${base}/api/v1/auth/login`, { headers: { Origin: base, 'Content-Type': 'application/json' },
    data: { username: 'estudiante.demo', password: '123456' } })
  await page.goto(base + '/')
  await page.waitForSelector('.map-area .unit-band', { timeout: 15000 })
  const mapa = await page.evaluate(() => {
    const caja = e => { const r = e.getBoundingClientRect(); return { x: r.x, y: r.y + scrollY, w: r.width, h: r.height } }
    const items = [...document.querySelectorAll('.map-area .unit-band, .map-area .trail-stop')].map(e => e.classList.contains('unit-band')
      ? { tipo: 'banda', texto: e.querySelector('.unit-band-title').textContent.trim(), caja: caja(e),
          comp: e.querySelector('.unit-band-competencies')?.textContent ?? '', avance: e.querySelector('.unit-band-progress').textContent.trim() }
      : { tipo: 'parada', texto: e.querySelector('.stop-title-text').textContent.trim(), caja: caja(e.querySelector('.lesson-node')),
          etiqueta: caja(e.querySelector('.node-label-pill')), chica: e.querySelector('.lesson-node').offsetWidth < 48 })
    return { items, desborda: document.documentElement.scrollWidth > innerWidth }
  })
  const grupos = []
  for (const it of mapa.items) {
    if (it.tipo === 'banda') grupos.push({ titulo: it.texto, paradas: [] })
    else if (grupos.length) grupos.at(-1).paradas.push(it.texto)
  }
  check(`[${nombre}] unidades y paradas dibujadas = orden esperado`, JSON.stringify(grupos) === JSON.stringify(esperado), JSON.stringify(grupos))
  const ys = mapa.items.map(i => i.caja.y)
  check(`[${nombre}] el orden visual (de arriba abajo) coincide con el orden del documento`, ys.every((y, i) => i === 0 || y > ys[i - 1]))
  const bandas = mapa.items.filter(i => i.tipo === 'banda'), paradas = mapa.items.filter(i => i.tipo === 'parada')
  const cruza = (a, b) => a.y < b.y + b.h && a.y + a.h > b.y && a.x < b.x + b.w && a.x + a.w > b.x
  check(`[${nombre}] ninguna banda tapa una parada o su etiqueta`, !bandas.some(b => paradas.some(p => cruza(b.caja, p.caja) || cruza(b.caja, p.etiqueta))))
  check(`[${nombre}] toda banda muestra competencias del currículo y avance «N de M»`, bandas.every(b => b.comp.includes('Currículo MINEDU:') && /^\d+ de \d+$/.test(b.avance)))
  check(`[${nombre}] paradas de al menos 48 px`, !paradas.some(p => p.chica))
  await ctx.close()
}
await browser.close()
console.log(fallas ? `\n${fallas} comprobaciones fallaron` : '\nTODO CORRECTO')
process.exit(fallas ? 1 : 0)
