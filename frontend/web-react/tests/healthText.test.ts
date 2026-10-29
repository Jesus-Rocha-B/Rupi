import test from 'node:test'
import assert from 'node:assert/strict'
import { healthHeadline, ms, serviceFacts, uptime } from '../src/features/admin/healthText.ts'

test('los tiempos se leen en ms o en segundos', () => {
  assert.equal(ms(84.4), '84 ms')
  assert.equal(ms(1530), '1.5 s')
  assert.equal(ms(null), '—')
  assert.equal(ms(0.4), '<1 ms')
})

test('el tiempo encendido se resume', () => {
  assert.equal(uptime(42), '42 s')
  assert.equal(uptime(600), '10 min')
  assert.equal(uptime(3 * 3600 + 120), '3 h 2 min')
  assert.equal(uptime(5 * 86400), '5 días')
})

test('el titular nombra lo que falla', () => {
  const ok = healthHeadline({ estado: 'OK', servicios: [{ clave: 'base', nombre: 'Base', estado: 'OK' }] })
  assert.equal(ok.title, 'Todo en orden')
  const falla = healthHeadline({ estado: 'FALLA', servicios: [
    { clave: 'base', nombre: 'Base de datos MySQL', estado: 'CAIDO' },
    { clave: 'api', nombre: 'API', estado: 'OK' },
  ] })
  assert.match(falla.body, /Base de datos MySQL/)
  assert.doesNotMatch(falla.body, /API,/)
})

test('la ficha de la API resume base, memoria y tiempo encendida', () => {
  const facts = serviceFacts({ clave: 'api', detalle: {
    segundosActiva: 120, baseDesdeApi: { state: 'UP', latencyMs: 3.2 }, memoria: { usedMemoryMb: 80, maxMemoryMb: 512 },
  } })
  assert.deepEqual(facts, [['Encendida hace', '2 min'], ['Base vista desde la API', 'responde en 3 ms'], ['Memoria', '80 de 512 MB']])
})

test('la ficha incluye métricas de CPU y RAM del servidor cuando están disponibles (HU-45)', () => {
  const facts = serviceFacts({ clave: 'api', detalle: {
    segundosActiva: 120,
    sistema: { cpuPercent: 24.5, usedMemoryMb: 4096, totalMemoryMb: 8192, memoryPercent: 50.0 },
    memoria: { usedMemoryMb: 95, maxMemoryMb: 512 },
  } })
  assert.deepEqual(facts, [
    ['Encendida hace', '2 min'],
    ['CPU del servidor', '24.5 %'],
    ['RAM del servidor', '4096 de 8192 MB (50 %)'],
    ['Memoria', '95 de 512 MB'],
  ])
})
