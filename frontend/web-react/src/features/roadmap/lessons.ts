export type LessonState = 'complete' | 'active' | 'locked'
export type Lesson = {
  id: number
  title: string
  context: string
  activity: string
  state: LessonState
  x: number
  y: number
  labelSide: 'left' | 'right'
}

export const lessons: Lesson[] = [
  { id: 1, title: 'Números en la plaza', context: 'Arcos de piedra', activity: 'Conteo · 8 min', state: 'complete', x: 50, y: 16, labelSide: 'right' },
  { id: 2, title: 'Sumas con retablos', context: 'Arte en miniatura', activity: 'Suma · 10 min', state: 'complete', x: 27, y: 25, labelSide: 'right' },
  { id: 3, title: 'La aventura de restar', context: 'Puestos de la plaza', activity: 'Resta · 8 min', state: 'active', x: 73, y: 34.5, labelSide: 'left' },
  { id: 4, title: 'Compras en el mercado', context: 'Feria de artesanía', activity: 'Desafío · 12 min', state: 'locked', x: 35, y: 43, labelSide: 'right' },
  { id: 5, title: 'Patrones en los tejidos', context: 'Colores y simetría', activity: 'Multiplicación · 9 min', state: 'locked', x: 68, y: 52, labelSide: 'left' },
  { id: 6, title: 'Medimos el camino', context: 'Laderas y andenes', activity: 'Medidas · 10 min', state: 'locked', x: 25, y: 61, labelSide: 'right' },
  { id: 7, title: 'Formas en la piedra', context: 'Antiguas construcciones', activity: 'Geometría · 8 min', state: 'locked', x: 75, y: 70, labelSide: 'left' },
  { id: 8, title: 'La hora de las campanas', context: 'Torres de la plaza', activity: 'Tiempo · 9 min', state: 'locked', x: 36, y: 79, labelSide: 'right' },
  { id: 9, title: 'Contamos los pasos', context: 'Sendero de altura', activity: 'Medidas · 10 min', state: 'locked', x: 67, y: 88, labelSide: 'left' },
  { id: 10, title: 'Reto de la cosecha', context: 'Huertas del valle', activity: 'Gran desafío · 15 min', state: 'locked', x: 50, y: 96, labelSide: 'left' },
]

export const createTrailPath = (points: { x: number; y: number }[]) => {
  const coords = points.map(({ x, y }) => ({ x: x * 6, y: y * 10 }))
  if (coords.length < 2) return ''

  return coords.reduce((path, point, index) => {
    if (index === 0) return `M ${point.x} ${point.y}`
    const previous = coords[index - 1]
    const beforePrevious = coords[index - 2] ?? previous
    const next = coords[index + 1] ?? point
    const controlOne = {
      x: previous.x + (point.x - beforePrevious.x) / 6,
      y: previous.y + (point.y - beforePrevious.y) / 6,
    }
    const controlTwo = {
      x: point.x - (next.x - previous.x) / 6,
      y: point.y - (next.y - previous.y) / 6,
    }
    return `${path} C ${controlOne.x} ${controlOne.y}, ${controlTwo.x} ${controlTwo.y}, ${point.x} ${point.y}`
  }, '')
}
