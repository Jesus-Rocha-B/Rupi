// Textos de la pestaña «Salud» (HU-45). Sin imports de React ni de Vite para probarlos con node --test.

interface HealthLike {
  estado: 'OK' | 'ATENCION' | 'FALLA'
  servicios: { clave: string; nombre: string; estado: string }[]
  alertas?: { id: string; nivel: string; titulo: string }[]
}

interface ServiceLike {
  clave: string
  detalle: Record<string, unknown>
}

export function ms(value: number | null | undefined) {
  if (value == null) return '—'
  if (value < 1) return '<1 ms'
  return value >= 1000 ? `${(value / 1000).toFixed(1)} s` : `${Math.round(value)} ms`
}

export function uptime(seconds: number) {
  if (seconds < 90) return `${Math.max(0, Math.round(seconds))} s`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 90) return `${minutes} min`
  const hours = Math.floor(minutes / 60)
  if (hours < 48) return `${hours} h ${minutes % 60} min`
  return `${Math.floor(hours / 24)} días`
}

export function healthHeadline(health: HealthLike) {
  const trouble = health.servicios.filter(item => item.estado !== 'OK').map(item => item.nombre)
  if (health.estado === 'OK') {
    return { title: 'Todo en orden', body: 'Las aulas pueden trabajar con normalidad.' }
  }
  if (health.estado === 'FALLA') {
    return {
      title: 'Hay una falla que afecta a las clases',
      body: trouble.length > 0
        ? `Revisa ahora: ${trouble.join(', ')}. Sin la base de datos o la API no se cargan rutas ni se guarda avance.`
        : 'Se detectaron fallos críticos o saturación en los servidores que requieren atención inmediata.',
    }
  }
  return {
    title: 'Conviene revisar',
    body: trouble.length > 0
      ? `${trouble.join(', ')}. Las clases siguen, pero algo podría empeorar.`
      : 'Se detectaron advertencias de rendimiento o consumo elevado en los servidores.',
  }
}

/** Datos que se muestran en la ficha de cada servicio, en el orden en que se leen. */
export function serviceFacts(service: ServiceLike): [string, string][] {
  const d = service.detalle
  const facts: [string, string][] = []
  if (service.clave === 'base') {
    if (typeof d.version === 'string') facts.push(['Versión', d.version])
    if (typeof d.conexiones === 'number') facts.push(['Conexiones abiertas', String(d.conexiones)])
  }
  if (service.clave === 'api') {
    if (typeof d.segundosActiva === 'number') facts.push(['Encendida hace', uptime(d.segundosActiva)])
    const sys = d.sistema as { cpuPercent?: number; usedMemoryMb?: number; totalMemoryMb?: number; memoryPercent?: number } | undefined
    if (sys && typeof sys.cpuPercent === 'number' && sys.cpuPercent >= 0) {
      facts.push(['CPU del servidor', `${sys.cpuPercent} %`])
    }
    if (sys && typeof sys.totalMemoryMb === 'number' && sys.totalMemoryMb > 0) {
      facts.push(['RAM del servidor', `${sys.usedMemoryMb ?? 0} de ${sys.totalMemoryMb} MB (${sys.memoryPercent ?? 0} %)`])
    }
    const db = d.baseDesdeApi as { state?: string; latencyMs?: number } | undefined
    if (db?.state) facts.push(['Base vista desde la API', db.state === 'UP' ? `responde en ${ms(db.latencyMs)}` : 'no responde'])
    const jvm = d.memoria as { usedMemoryMb?: number; maxMemoryMb?: number } | undefined
    if (jvm?.maxMemoryMb) facts.push(['Memoria', `${jvm.usedMemoryMb} de ${jvm.maxMemoryMb} MB`])
  }
  if (service.clave === 'consola') {
    const rec = d.recursos as { cpuPercent?: number; memoria?: { usadaMb: number; totalMb: number; porcentaje: number } } | undefined
    if (rec && typeof rec.cpuPercent === 'number') {
      facts.push(['CPU del host', `${rec.cpuPercent} %`])
    }
    if (rec?.memoria && rec.memoria.totalMb > 0) {
      facts.push(['RAM física', `${rec.memoria.usadaMb} de ${rec.memoria.totalMb} MB (${rec.memoria.porcentaje} %)`])
    }
  }
  if (service.clave === 'ia') {
    if (typeof d.model === 'string') facts.push(['Modelo', d.model])
    if (typeof d.geminiConfigured === 'boolean') facts.push(['Proveedor configurado', d.geminiConfigured ? 'Sí' : 'No'])
  }
  return facts
}
