export const REVIEW_SUBJECTS = [
  { code: 'MATEMATICA', name: 'Matemática' },
  { code: 'COMUNICACION', name: 'Comunicación' },
  { code: 'CYT', name: 'Ciencia y Tecnología' },
]

export function reviewSubject(code: string) {
  const key = code.trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase().replace(/[\s_-]+/g, '')
  const canonical = ({ MAT: 'MATEMATICA', MATEMATICAS: 'MATEMATICA', COM: 'COMUNICACION', CIENCIAYTECNOLOGIA: 'CYT', CIENCIATECNOLOGIA: 'CYT', CT: 'CYT' } as Record<string, string>)[key] ?? key
  return REVIEW_SUBJECTS.find(s => s.code === canonical)
}
