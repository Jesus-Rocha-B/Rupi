// HU-12: abre el mapa en el navegador, descarga una unidad desde el panel «Llévate tu unidad» y revisa el zip.
// Solo entorno local, con Rupi levantado (./iniciar-rupi.sh). Playwright no es dependencia del repo:
//   PLAYWRIGHT_DIR=/tmp/pw node scripts/verify_hu12_pantalla.mjs [http://localhost:5173] [carpeta-capturas]
import { createRequire } from 'node:module'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { tmpdir } from 'node:os'

const dir = process.env.PLAYWRIGHT_DIR
if (!dir) { console.error('Define PLAYWRIGHT_DIR (ver verify_hu04_pantalla.mjs).'); process.exit(2) }
const { firefox } = createRequire(join(dir, 'package.json'))('playwright')
const base = process.argv[2] ?? 'http://localhost:5173'
const capturas = process.argv[3] ?? tmpdir()
if (!/^http:\/\/(localhost|127\.0\.0\.1):/.test(base)) { console.error('Solo contra la web local.'); process.exit(2) }
let fallas = 0
const check = (nombre, ok, detalle = '') => { console.log(ok ? 'OK:' : 'FALLA:', nombre, ok ? '' : detalle); if (!ok) fallas++ }

const browser = await firefox.launch()
for (const [nombre, w, h] of [['escritorio 1280 px', 1280, 900], ['móvil 375 px', 375, 800]]) {
  const ctx = await browser.newContext({ viewport: { width: w, height: h }, acceptDownloads: true })
  const page = await ctx.newPage()
  await ctx.request.post(`${base}/api/v1/auth/login`, { headers: { Origin: base, 'Content-Type': 'application/json' },
    data: { username: 'estudiante.demo', password: '123456' } })
  await page.goto(base + '/')
  const panel = page.locator('.unit-downloads')
  await panel.waitFor({ timeout: 15000 })
  await panel.scrollIntoViewIfNeeded()

  await page.getByRole('combobox', { name: 'Quiero descargar' }).selectOption('unit')
  const datos = await page.evaluate(() => {
    const bandas = [...document.querySelectorAll('.map-area .unit-band-title')].map(e => e.textContent.trim())
    const tarjetas = [...document.querySelectorAll('.unit-download-title')].map(e => e.textContent.trim())
    const botones = [...document.querySelectorAll('.unit-download-btn')].map(b => { const r = b.getBoundingClientRect(); return [r.width, r.height] })
    const fuera = [...document.querySelectorAll('.unit-downloads, .unit-downloads *')].some(e => e.getBoundingClientRect().right > innerWidth + 1)
    return { bandas, tarjetas, botones, desborda: fuera }
  })
  check(`[${nombre}] una tarjeta por unidad del mapa, en el mismo orden`, JSON.stringify(datos.bandas) === JSON.stringify(datos.tarjetas),
    JSON.stringify(datos))
  check(`[${nombre}] botones de al menos 48 × 48 px`, datos.botones.length > 0 && datos.botones.every(([bw, bh]) => bw >= 48 && bh >= 48))
  check(`[${nombre}] el panel cabe en el ancho de la pantalla`, !datos.desborda)

  const boton = page.locator('.unit-download-btn').first()
  await boton.focus()
  const [descarga] = await Promise.all([page.waitForEvent('download', { timeout: 30000 }), page.keyboard.press('Enter')])
  const archivo = await descarga.path()
  const zip = readFileSync(archivo)
  check(`[${nombre}] la descarga con teclado entrega un .zip`, descarga.suggestedFilename().endsWith('.zip') && zip.subarray(0, 2).toString() === 'PK',
    descarga.suggestedFilename())
  const estado = page.locator('.unit-download-card').first().locator('.unit-download-status')
  await estado.filter({ hasText: 'Archivo verificado' }).waitFor({ timeout: 10000 })
  check(`[${nombre}] el mensaje confirma que llegó completo`, (await estado.textContent()).includes('¡Archivo verificado!'))
  await panel.screenshot({ path: join(capturas, `hu12-panel-${w}.png`) })
  await ctx.close()
}
await browser.close()
console.log(fallas ? `\n${fallas} comprobaciones fallaron` : '\nTODO CORRECTO')
process.exit(fallas ? 1 : 0)
