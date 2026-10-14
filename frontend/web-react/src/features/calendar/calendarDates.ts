/** Los horarios escolares se interpretan siempre en Perú, independientemente del dispositivo. */
export function limaInput(iso: string): string {
  return new Date(new Date(iso).getTime() - 5 * 3600000).toISOString().slice(0, 16)
}
export function limaInstant(value: string): string { return `${value}:00-05:00` }
export function monthCells(month: string): (string | null)[] {
  const [year, number] = month.split('-').map(Number)
  const offset = (new Date(Date.UTC(year, number - 1, 1)).getUTCDay() + 6) % 7
  const count = new Date(Date.UTC(year, number, 0)).getUTCDate()
  return [...Array<null>(offset).fill(null), ...Array.from({length: count}, (_, i) => `${month}-${String(i + 1).padStart(2, '0')}`)]
}
export function occursOn(start: string, end: string, day: string): boolean {
  return limaInput(start).slice(0, 10) <= day && limaInput(end).slice(0, 10) >= day
}
export function shiftMonth(month: string, amount: number): string {
  const [year, number] = month.split('-').map(Number)
  return new Date(Date.UTC(year, number - 1 + amount, 1)).toISOString().slice(0, 7)
}
