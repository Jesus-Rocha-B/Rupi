// HU-15: el docente elige la ayuda del tutor Rupi para su aula y el estudiante la ve al abrir el chat.
// Solo entorno local, con Rupi levantado (./iniciar-rupi.sh) y la migración V44 aplicada.
//   PLAYWRIGHT_DIR=/tmp/pw node scripts/verify_hu15_pantalla.mjs [http://localhost:5173] [carpeta-capturas]
// Deja el aula «3.° Primaria A» con pistas guiadas, el valor predeterminado.
import { createRequire } from 'node:module'
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
const login = (ctx, username) => ctx.request.post(`${base}/api/v1/auth/login`, {
  headers: { Origin: base, 'Content-Type': 'application/json' }, data: { username, password: '123456' } })
const desborda = page => page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1)

const ROOM = 'e3000000-0000-4000-8000-000000000003'
const ponerModo = async (browser, mode) => {
  const ctx = await browser.newContext()
  await login(ctx, 'profesor.demo')
  const r = await ctx.request.put(`${base}/api/v1/teacher/tutor-support/classrooms/${ROOM}`,
    { headers: { Origin: base, 'Content-Type': 'application/json' }, data: { mode } })
  const ok = r.ok() && (await r.json()).mode === mode
  await ctx.close()
  return ok
}

const browser = await firefox.launch()
for (const [nombre, w, h] of [['escritorio 1280 px', 1280, 900], ['móvil 375 px', 375, 800]]) {
  await ponerModo(browser, 'PISTAS_GUIADAS')
  // Docente: Cuaderno docente → aula de 3.° → explicación directa.
  const docente = await browser.newContext({ viewport: { width: w, height: h } })
  await login(docente, 'profesor.demo')
  const page = await docente.newPage()
  await page.goto(base + '/')
  await page.locator('.open-editor-btn').first().click()
  await page.getByRole('button', { name: /Reportes de resultados/ }).click()
  await page.getByRole('combobox', { name: 'Aula' }).selectOption({ label: '3.° Primaria A · 3.º de primaria' })
  const tarjeta = page.locator('.tutor-support')
  await tarjeta.waitFor({ timeout: 15000 })
  const directa = tarjeta.getByRole('radio', { name: /Explicación directa/ })
  await directa.focus()
  await page.keyboard.press('Space')
  check(`[${nombre}] la opción se elige con el teclado`, await directa.isChecked())
  const guardar = tarjeta.getByRole('button', { name: 'Guardar ayuda del tutor' })
  const caja = await guardar.boundingBox()
  check(`[${nombre}] botón de guardar de al menos 48 px de alto`, caja && caja.height >= 48, JSON.stringify(caja))
  await guardar.click()
  await tarjeta.getByText('Guardado. Se aplica en la próxima pregunta').waitFor({ timeout: 10000 })
  check(`[${nombre}] confirma el guardado y desactiva el botón`, await guardar.isDisabled())
  check(`[${nombre}] el Cuaderno docente no desborda`, !(await desborda(page)))
  await tarjeta.screenshot({ path: join(capturas, `hu15-docente-${w}.png`) })
  await page.reload()
  await page.locator('.open-editor-btn').first().click()
  await page.getByRole('button', { name: /Reportes de resultados/ }).click()
  await page.getByRole('combobox', { name: 'Aula' }).selectOption({ label: '3.° Primaria A · 3.º de primaria' })
  await page.locator('.tutor-support').waitFor({ timeout: 15000 })
  check(`[${nombre}] el modo queda guardado al volver`, await page.locator('.tutor-support').getByRole('radio', { name: /Explicación directa/ }).isChecked())
  await docente.close()

  // Estudiante del aula: abre el chat de su ruta de 3.° y ve el modo.
  const alumno = await browser.newContext({ viewport: { width: w, height: h } })
  await login(alumno, 'diego.ccori')
  const vista = await alumno.newPage()
  await vista.goto(base + '/')
  await vista.getByRole('button', { name: 'Abrir tutor de ayuda Rupi' }).click({ timeout: 20000 })
  const nota = vista.locator('.rupi-mode-note')
  await nota.waitFor({ timeout: 10000 })
  const texto = await nota.textContent()
  check(`[${nombre}] el estudiante ve la explicación directa de su aula`, texto.includes('paso a paso') && texto.includes('3.° Primaria A'), texto)
  check(`[${nombre}] la vista del estudiante no desborda`, !(await desborda(vista)))
  await vista.locator('.rupi-chat-card').screenshot({ path: join(capturas, `hu15-chat-${w}.png`) })
  await alumno.close()
}

// Restaurar el valor predeterminado.
check('el aula vuelve a pistas guiadas', await ponerModo(browser, 'PISTAS_GUIADAS'))
await browser.close()
console.log(fallas ? `\n${fallas} comprobaciones fallaron` : '\nTODO CORRECTO')
process.exit(fallas ? 1 : 0)
