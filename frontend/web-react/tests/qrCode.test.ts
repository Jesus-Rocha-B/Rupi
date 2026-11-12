import assert from 'node:assert/strict'
import test from 'node:test'
import { buildQrMatrix } from '../src/utils/qrCode.ts'

test('genera una matriz QR válida para un URI otpauth://totp estándar', () => {
  const uri = 'otpauth://totp/Rupi:profesor.demo?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=Rupi&algorithm=SHA1&digits=6&period=30'
  const matrix = buildQrMatrix(uri)
  const n = matrix.length

  // Tamaño QR = 4*v + 17 (con v >= 1)
  assert.equal((n - 17) % 4, 0)
  assert.ok(n >= 41) // ~112 bytes -> versión 6 o 7

  // Verificar esquinas del finder top-left (7x7)
  assert.equal(matrix[0][0], true)
  assert.equal(matrix[0][6], true)
  assert.equal(matrix[6][0], true)
  assert.equal(matrix[1][1], false)
  assert.equal(matrix[3][3], true)

  // Módulo oscuro fijo en (n - 8, 8)
  assert.equal(matrix[n - 8][8], true)

  // Patrones de temporización alternados en fila 6
  assert.equal(matrix[6][8], true)
  assert.equal(matrix[6][9], false)
  assert.equal(matrix[6][10], true)
})
