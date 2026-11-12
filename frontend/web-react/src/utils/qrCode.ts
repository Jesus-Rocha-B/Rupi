// Codificador QR ISO/IEC 18004 (Modo Byte, corrección de errores M, versiones 1-10) sin dependencias externas.

interface VersionSpec {
  version: number
  ecPerBlock: number
  g1Blocks: number
  g1Data: number
  g2Blocks: number
  g2Data: number
  align: number[]
  remainderBits: number
}

const VERSIONS_M: VersionSpec[] = [
  { version: 1, ecPerBlock: 10, g1Blocks: 1, g1Data: 16, g2Blocks: 0, g2Data: 0, align: [], remainderBits: 0 },
  { version: 2, ecPerBlock: 16, g1Blocks: 1, g1Data: 28, g2Blocks: 0, g2Data: 0, align: [6, 18], remainderBits: 7 },
  { version: 3, ecPerBlock: 26, g1Blocks: 1, g1Data: 44, g2Blocks: 0, g2Data: 0, align: [6, 22], remainderBits: 7 },
  { version: 4, ecPerBlock: 18, g1Blocks: 2, g1Data: 32, g2Blocks: 0, g2Data: 0, align: [6, 26], remainderBits: 7 },
  { version: 5, ecPerBlock: 24, g1Blocks: 2, g1Data: 43, g2Blocks: 0, g2Data: 0, align: [6, 30], remainderBits: 7 },
  { version: 6, ecPerBlock: 16, g1Blocks: 4, g1Data: 27, g2Blocks: 0, g2Data: 0, align: [6, 34], remainderBits: 7 },
  { version: 7, ecPerBlock: 18, g1Blocks: 4, g1Data: 31, g2Blocks: 0, g2Data: 0, align: [6, 22, 38], remainderBits: 0 },
  { version: 8, ecPerBlock: 22, g1Blocks: 2, g1Data: 38, g2Blocks: 2, g2Data: 39, align: [6, 24, 42], remainderBits: 0 },
  { version: 9, ecPerBlock: 22, g1Blocks: 3, g1Data: 36, g2Blocks: 2, g2Data: 37, align: [6, 26, 46], remainderBits: 0 },
  { version: 10, ecPerBlock: 26, g1Blocks: 4, g1Data: 43, g2Blocks: 1, g2Data: 44, align: [6, 28, 50], remainderBits: 0 },
]

const GF_EXP = new Uint8Array(512)
const GF_LOG = new Uint8Array(256)
{
  let x = 1
  for (let i = 0; i < 255; i++) {
    GF_EXP[i] = x
    GF_LOG[x] = i
    x <<= 1
    if (x & 0x100) x ^= 0x11d
  }
  for (let i = 255; i < 512; i++) GF_EXP[i] = GF_EXP[i - 255]
}

function gfMul(a: number, b: number): number {
  return a === 0 || b === 0 ? 0 : GF_EXP[GF_LOG[a] + GF_LOG[b]]
}

function rsGenerator(degree: number): Uint8Array {
  let poly = new Uint8Array([1])
  for (let i = 0; i < degree; i++) {
    const next = new Uint8Array(poly.length + 1)
    const factor = GF_EXP[i]
    for (let j = 0; j < poly.length; j++) {
      next[j] ^= poly[j]
      next[j + 1] ^= gfMul(poly[j], factor)
    }
    poly = next
  }
  return poly
}

function rsRemainder(data: Uint8Array, ecCount: number): Uint8Array {
  const gen = rsGenerator(ecCount)
  const buf = new Uint8Array(data.length + ecCount)
  buf.set(data)
  for (let i = 0; i < data.length; i++) {
    const coef = buf[i]
    if (coef !== 0) {
      for (let j = 0; j < gen.length; j++) {
        buf[i + j] ^= gfMul(gen[j], coef)
      }
    }
  }
  return buf.slice(data.length)
}

function pickVersion(byteLength: number): VersionSpec {
  for (const spec of VERSIONS_M) {
    const dataBytes = spec.g1Blocks * spec.g1Data + spec.g2Blocks * spec.g2Data
    const headerBits = 4 + (spec.version >= 10 ? 16 : 8)
    const maxInputBytes = Math.floor((dataBytes * 8 - headerBits) / 8)
    if (byteLength <= maxInputBytes) return spec
  }
  throw new Error('Contenido demasiado largo para el generador QR (máximo 213 bytes).')
}

function encodeCodewords(bytes: Uint8Array, spec: VersionSpec): number[] {
  const totalDataBytes = spec.g1Blocks * spec.g1Data + spec.g2Blocks * spec.g2Data
  const bits: number[] = []
  const pushBits = (val: number, count: number) => {
    for (let i = count - 1; i >= 0; i--) bits.push((val >> i) & 1)
  }

  pushBits(0b0100, 4)
  pushBits(bytes.length, spec.version >= 10 ? 16 : 8)
  for (const b of bytes) pushBits(b, 8)
  const capacityBits = totalDataBytes * 8
  for (let i = 0; i < 4 && bits.length < capacityBits; i++) bits.push(0)
  while (bits.length % 8 !== 0) bits.push(0)

  const dataWords: number[] = []
  for (let i = 0; i < bits.length; i += 8) {
    let word = 0
    for (let j = 0; j < 8; j++) word = (word << 1) | bits[i + j]
    dataWords.push(word)
  }
  const pads = [0xec, 0x11]
  for (let i = 0; dataWords.length < totalDataBytes; i++) {
    dataWords.push(pads[i % 2])
  }

  const dataBlocks: Uint8Array[] = []
  const ecBlocks: Uint8Array[] = []
  let offset = 0
  for (let b = 0; b < spec.g1Blocks; b++) {
    const slice = new Uint8Array(dataWords.slice(offset, offset + spec.g1Data))
    offset += spec.g1Data
    dataBlocks.push(slice)
    ecBlocks.push(rsRemainder(slice, spec.ecPerBlock))
  }
  for (let b = 0; b < spec.g2Blocks; b++) {
    const slice = new Uint8Array(dataWords.slice(offset, offset + spec.g2Data))
    offset += spec.g2Data
    dataBlocks.push(slice)
    ecBlocks.push(rsRemainder(slice, spec.ecPerBlock))
  }

  const codewords: number[] = []
  const maxDataLen = Math.max(spec.g1Data, spec.g2Data)
  for (let i = 0; i < maxDataLen; i++) {
    for (const block of dataBlocks) if (i < block.length) codewords.push(block[i])
  }
  for (let i = 0; i < spec.ecPerBlock; i++) {
    for (const block of ecBlocks) codewords.push(block[i])
  }
  return codewords
}

function formatInfoBits(mask: number): number {
  const data = (0b00 << 3) | mask // EC level M = 00
  let rem = data << 10
  for (let bit = 14; bit >= 10; bit--) {
    if (rem & (1 << bit)) rem ^= 0b10100110111 << (bit - 10)
  }
  return ((data << 10) | rem) ^ 0b101010000010010
}

function versionInfoBits(version: number): number {
  let rem = version << 12
  for (let bit = 17; bit >= 12; bit--) {
    if (rem & (1 << bit)) rem ^= 0b1111100100101 << (bit - 12)
  }
  return (version << 12) | rem
}

function placePatterns(spec: VersionSpec): { modules: (boolean | null)[][]; fixed: boolean[][] } {
  const size = spec.version * 4 + 17
  const modules: (boolean | null)[][] = Array.from({ length: size }, () => Array(size).fill(null))
  const fixed: boolean[][] = Array.from({ length: size }, () => Array(size).fill(false))

  const setFixed = (r: number, c: number, val: boolean) => {
    if (r < 0 || c < 0 || r >= size || c >= size) return
    modules[r][c] = val
    fixed[r][c] = true
  }

  const drawFinder = (top: number, left: number) => {
    for (let dr = -1; dr <= 7; dr++) {
      for (let dc = -1; dc <= 7; dc++) {
        const inBox = dr >= 0 && dr <= 6 && dc >= 0 && dc <= 6
        const onRing = dr === 0 || dr === 6 || dc === 0 || dc === 6
        const inCore = dr >= 2 && dr <= 4 && dc >= 2 && dc <= 4
        setFixed(top + dr, left + dc, inBox && (onRing || inCore))
      }
    }
  }

  drawFinder(0, 0)
  drawFinder(0, size - 7)
  drawFinder(size - 7, 0)

  for (let i = 8; i < size - 8; i++) {
    setFixed(6, i, i % 2 === 0)
    setFixed(i, 6, i % 2 === 0)
  }

  // Reserva zona de información de formato
  for (let i = 0; i < 9; i++) {
    if (!fixed[8][i]) setFixed(8, i, false)
    if (!fixed[i][8]) setFixed(i, 8, false)
  }
  for (let i = 0; i < 8; i++) {
    setFixed(8, size - 1 - i, false)
    if (i < 7) setFixed(size - 1 - i, 8, false)
  }
  setFixed(size - 8, 8, true) // Dark module

  // Patrones de alineamiento
  for (const ar of spec.align) {
    for (const ac of spec.align) {
      if (fixed[ar][ac]) continue
      for (let dr = -2; dr <= 2; dr++) {
        for (let dc = -2; dc <= 2; dc++) {
          const val = Math.abs(dr) === 2 || Math.abs(dc) === 2 || (dr === 0 && dc === 0)
          setFixed(ar + dr, ac + dc, val)
        }
      }
    }
  }

  // Información de versión para V7+
  if (spec.version >= 7) {
    const vBits = versionInfoBits(spec.version)
    for (let i = 0; i < 18; i++) {
      const bit = ((vBits >> i) & 1) === 1
      const r = Math.floor(i / 3)
      const c = size - 11 + (i % 3)
      setFixed(r, c, bit)
      setFixed(c, r, bit)
    }
  }

  return { modules, fixed }
}

function applyDataAndMask(spec: VersionSpec, codewords: number[], mask: number): boolean[][] {
  const size = spec.version * 4 + 17
  const { modules, fixed } = placePatterns(spec)

  const bitStream: number[] = []
  for (const w of codewords) for (let b = 7; b >= 0; b--) bitStream.push((w >> b) & 1)
  for (let b = 0; b < spec.remainderBits; b++) bitStream.push(0)

  const maskFn = (r: number, c: number) => {
    switch (mask) {
      case 0: return (r + c) % 2 === 0
      case 1: return r % 2 === 0
      case 2: return c % 3 === 0
      case 3: return (r + c) % 3 === 0
      case 4: return (Math.floor(r / 2) + Math.floor(c / 3)) % 2 === 0
      case 5: return ((r * c) % 2) + ((r * c) % 3) === 0
      case 6: return (((r * c) % 2) + ((r * c) % 3)) % 2 === 0
      default: return (((r + c) % 2) + ((r * c) % 3)) % 2 === 0
    }
  }

  let bitIdx = 0
  let up = true
  for (let right = size - 1; right >= 1; right -= 2) {
    if (right === 6) right = 5
    for (let step = 0; step < size; step++) {
      const r = up ? size - 1 - step : step
      for (const c of [right, right - 1]) {
        if (fixed[r][c]) continue
        const bit = bitIdx < bitStream.length ? bitStream[bitIdx++] === 1 : false
        modules[r][c] = maskFn(r, c) ? !bit : bit
      }
    }
    up = !up
  }

  // Escribe los bits de formato alrededor de los tres finders
  const fmt = formatInfoBits(mask)
  for (let i = 0; i < 15; i++) {
    const bit = ((fmt >> i) & 1) === 1
    if (i < 6) modules[8][i] = bit
    else if (i < 8) modules[8][i + 1] = bit
    else modules[8][size - 15 + i] = bit

    if (i < 7) modules[size - 1 - i][8] = bit
    else if (i === 7) modules[8][8] = bit
    else if (i === 8) modules[7][8] = bit
    else modules[14 - i][8] = bit
  }
  modules[size - 8][8] = true

  return modules.map(row => row.map(Boolean))
}

function penaltyScore(grid: boolean[][]): number {
  const n = grid.length
  let score = 0
  for (let r = 0; r < n; r++) {
    let runColor = grid[r][0]
    let runLen = 1
    for (let c = 1; c < n; c++) {
      if (grid[r][c] === runColor) {
        runLen++
      } else {
        if (runLen >= 5) score += 3 + (runLen - 5)
        runColor = grid[r][c]
        runLen = 1
      }
    }
    if (runLen >= 5) score += 3 + (runLen - 5)
  }
  for (let c = 0; c < n; c++) {
    let runColor = grid[0][c]
    let runLen = 1
    for (let r = 1; r < n; r++) {
      if (grid[r][c] === runColor) {
        runLen++
      } else {
        if (runLen >= 5) score += 3 + (runLen - 5)
        runColor = grid[r][c]
        runLen = 1
      }
    }
    if (runLen >= 5) score += 3 + (runLen - 5)
  }
  for (let r = 0; r < n - 1; r++) {
    for (let c = 0; c < n - 1; c++) {
      const v = grid[r][c]
      if (v === grid[r][c + 1] && v === grid[r + 1][c] && v === grid[r + 1][c + 1]) score += 3
    }
  }
  let dark = 0
  for (let r = 0; r < n; r++) for (let c = 0; c < n; c++) if (grid[r][c]) dark++
  const pct = (dark * 100) / (n * n)
  score += Math.floor(Math.abs(pct - 50) / 5) * 10
  return score
}

export function buildQrMatrix(text: string): boolean[][] {
  const bytes = new TextEncoder().encode(text)
  const spec = pickVersion(bytes.length)
  const codewords = encodeCodewords(bytes, spec)
  let best = applyDataAndMask(spec, codewords, 0)
  let bestScore = penaltyScore(best)
  for (let mask = 1; mask < 8; mask++) {
    const candidate = applyDataAndMask(spec, codewords, mask)
    const candidateScore = penaltyScore(candidate)
    if (candidateScore < bestScore) {
      best = candidate
      bestScore = candidateScore
    }
  }
  return best
}
