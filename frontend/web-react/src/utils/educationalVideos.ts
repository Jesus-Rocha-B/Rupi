/**
 * Videos educativos reales y verificados de YouTube para Primaria en Perú
 * (Happy Learning, ChiquitinesTV, cuentos tradicionales andinos).
 * Configurado con parámetros seguros para evitar Error 153 en localhost y entornos web.
 */

export interface EducationalVideo {
  id: string
  title: string
  embedUrl: string
  watchUrl: string
  description?: string
  isDirectVideo?: boolean
}

function buildEmbedUrl(videoId: string): string {
  const origin = typeof window !== 'undefined' && window.location.origin ? window.location.origin : ''
  const originParam = origin ? `&origin=${encodeURIComponent(origin)}` : ''
  // Usar standard youtube.com/embed con enablejsapi y origin para evitar Error 153
  return `https://www.youtube.com/embed/${videoId}?enablejsapi=1${originParam}&rel=0&modestbranding=1`
}

export const VERIFIED_EDUCATIONAL_VIDEOS: Record<string, EducationalVideo> = {
  conteo: {
    id: 'fVh43cTZzTE',
    title: 'Los Números del 1 al 100 - Conteo y Matemáticas',
    embedUrl: buildEmbedUrl('fVh43cTZzTE'),
    watchUrl: 'https://www.youtube.com/watch?v=fVh43cTZzTE',
    description: 'Aprende a contar del 1 al 100 de forma divertida y rítmica.',
  },
  suma: {
    id: 'oexd_Dfic_Q',
    title: 'Aprendiendo a sumar. La Suma | Happy Learning',
    embedUrl: buildEmbedUrl('oexd_Dfic_Q'),
    watchUrl: 'https://www.youtube.com/watch?v=oexd_Dfic_Q',
    description: 'Descubre qué es sumar, cómo juntar elementos y resolver sumas escolares.',
  },
  resta: {
    id: '42vjqtleG9E',
    title: 'Aprendiendo a restar. La Resta | Happy Learning',
    embedUrl: buildEmbedUrl('42vjqtleG9E'),
    watchUrl: 'https://www.youtube.com/watch?v=42vjqtleG9E',
    description: 'Aprende a quitar, comparar y resolver restas paso a paso.',
  },
  dinero: {
    id: 'VzBJ_FV6eoI',
    title: 'La historia del dinero y las monedas | Happy Learning',
    embedUrl: buildEmbedUrl('VzBJ_FV6eoI'),
    watchUrl: 'https://www.youtube.com/watch?v=VzBJ_FV6eoI',
    description: 'Comprende el valor del dinero, las compras y el intercambio comercial.',
  },
  patron: {
    id: 'dzgnqza-2cU',
    title: 'Secuencias y patrones numéricos para primaria',
    embedUrl: buildEmbedUrl('dzgnqza-2cU'),
    watchUrl: 'https://www.youtube.com/watch?v=dzgnqza-2cU',
    description: 'Identifica patrones repetitivos y secuencias lógicas.',
  },
  longitud: {
    id: 'kzrplJ1jvko',
    title: 'La longitud y su unidad de medida: El metro | Happy Learning',
    embedUrl: buildEmbedUrl('kzrplJ1jvko'),
    watchUrl: 'https://www.youtube.com/watch?v=kzrplJ1jvko',
    description: 'Mide distancias, objetos y aprende el uso del metro y el centímetro.',
  },
  geometria: {
    id: 'SXONzObzFk0',
    title: 'Las figuras geométricas planas | Vocabulario de Primaria',
    embedUrl: buildEmbedUrl('SXONzObzFk0'),
    watchUrl: 'https://www.youtube.com/watch?v=SXONzObzFk0',
    description: 'Círculos, cuadrados, triángulos y rectángulos en nuestro entorno.',
  },
  reloj: {
    id: 'tBN6HGyuGwY',
    title: 'El reloj y las horas | Happy Learning',
    embedUrl: buildEmbedUrl('tBN6HGyuGwY'),
    watchUrl: 'https://www.youtube.com/watch?v=tBN6HGyuGwY',
    description: 'Aprende a leer la hora en relojes de agujas y digitales.',
  },
  cuento: {
    id: 'K7Bvi6zMDWk',
    title: 'Cuento andino: El zorro y el cuy',
    embedUrl: buildEmbedUrl('K7Bvi6zMDWk'),
    watchUrl: 'https://www.youtube.com/watch?v=K7Bvi6zMDWk',
    description: 'Relato tradicional andino sobre la astucia, el respeto y la perseverancia.',
  },
  lectura: {
    id: 'pCyPzdzrvEI',
    title: 'Comprensión lectora y textos narrativos',
    embedUrl: buildEmbedUrl('pCyPzdzrvEI'),
    watchUrl: 'https://www.youtube.com/watch?v=pCyPzdzrvEI',
    description: 'Aprende a identificar la idea principal y comprender lo que lees.',
  },
  plantas: {
    id: 'ThbSPNlRz0o',
    title: 'El reino de las plantas y su clasificación | Happy Learning',
    embedUrl: buildEmbedUrl('ThbSPNlRz0o'),
    watchUrl: 'https://www.youtube.com/watch?v=ThbSPNlRz0o',
    description: 'Las partes de la planta, fotosíntesis y flora de nuestro país.',
  },
  ecosistema: {
    id: 'SsRdLoYOMQA',
    title: '¡Aprende sobre los Ecosistemas! | Happy Learning',
    embedUrl: buildEmbedUrl('SsRdLoYOMQA'),
    watchUrl: 'https://www.youtube.com/watch?v=SsRdLoYOMQA',
    description: 'Seres vivos, medio ambiente y el equilibrio de la naturaleza.',
  },
  sentidos: {
    id: 'fedTu_X_s70',
    title: 'Los cinco sentidos del cuerpo humano | Smile and Learn',
    embedUrl: buildEmbedUrl('fedTu_X_s70'),
    watchUrl: 'https://www.youtube.com/watch?v=fedTu_X_s70',
    description: 'Descubre los órganos de los sentidos: vista, oído, olfato, gusto y tacto.',
  },
  cuerpo: {
    id: 'pNu_pU_G1H8',
    title: 'El cuerpo humano para niños | Smile and Learn',
    embedUrl: buildEmbedUrl('pNu_pU_G1H8'),
    watchUrl: 'https://www.youtube.com/watch?v=pNu_pU_G1H8',
    description: 'Aprende las partes del cuerpo humano, los músculos, huesos y su cuidado.',
  },
  animales: {
    id: 'uQo53tT2I8g',
    title: 'Los animales vertebrados e invertebrados | Happy Learning',
    embedUrl: buildEmbedUrl('uQo53tT2I8g'),
    watchUrl: 'https://www.youtube.com/watch?v=uQo53tT2I8g',
    description: 'Conoce los grupos de animales y la biodiversidad de nuestro entorno.',
  },
  fracciones: {
    id: 'c9c_8x6zTdA',
    title: 'Las Fracciones para niños | Smile and Learn',
    embedUrl: buildEmbedUrl('c9c_8x6zTdA'),
    watchUrl: 'https://www.youtube.com/watch?v=c9c_8x6zTdA',
    description: 'Comprende qué es una fracción, el numerador y el denominador.',
  },
  multiplicacion: {
    id: 'YFtEaVw5k1A',
    title: 'La Multiplicación | Aprender a multiplicar | Happy Learning',
    embedUrl: buildEmbedUrl('YFtEaVw5k1A'),
    watchUrl: 'https://www.youtube.com/watch?v=YFtEaVw5k1A',
    description: 'Aprende el concepto de la multiplicación y cómo calcularla fácilmente.',
  },
  division: {
    id: 'PCRCrdJbaCM',
    title: 'La División | Aprender a dividir | Happy Learning',
    embedUrl: buildEmbedUrl('PCRCrdJbaCM'),
    watchUrl: 'https://www.youtube.com/watch?v=PCRCrdJbaCM',
    description: 'Descubre cómo repartir en partes iguales con la división.',
  },
  poemas: {
    id: '3R4QxN37z1E',
    title: 'Las Rimas y la Poesía para niños | Smile and Learn',
    embedUrl: buildEmbedUrl('3R4QxN37z1E'),
    watchUrl: 'https://www.youtube.com/watch?v=3R4QxN37z1E',
    description: 'Aprende a rimar palabras, leer versos y crear hermosos poemas escolares.',
  },
}

/**
 * Convierte cualquier enlace de YouTube en URL de incrustación segura.
 */
export function getYouTubeEmbedUrl(urlStr: string | null | undefined): string | null {
  if (!urlStr) return null
  try {
    const url = new URL(urlStr)
    let videoId: string | null = null
    if (url.hostname.includes('youtube.com')) {
      if (url.pathname.startsWith('/embed/')) {
        videoId = url.pathname.replace('/embed/', '')
      } else {
        videoId = url.searchParams.get('v')
      }
    } else if (url.hostname.includes('youtu.be')) {
      videoId = url.pathname.replace('/', '')
    }
    if (videoId) {
      // Limpiar parámetros adicionales
      videoId = videoId.split('?')[0].split('&')[0]
      return buildEmbedUrl(videoId)
    }
    return null
  } catch {
    return null
  }
}

/**
 * Extrae el ID puro de YouTube a partir de cualquier formato de URL.
 */
export function extractYouTubeId(urlStr: string | null | undefined): string | null {
  if (!urlStr) return null
  try {
    const url = new URL(urlStr)
    if (url.hostname.includes('youtube.com')) {
      if (url.pathname.startsWith('/embed/')) {
        return url.pathname.replace('/embed/', '').split('?')[0].split('&')[0]
      }
      return url.searchParams.get('v')
    }
    if (url.hostname.includes('youtu.be')) {
      return url.pathname.replace('/', '').split('?')[0].split('&')[0]
    }
    return null
  } catch {
    return null
  }
}

/**
 * Resuelve un video educativo real para cualquier actividad o parada escolar.
 */
export function resolveEducationalVideo(
  blocks?: Array<{ url?: string | null; type?: string | null; text?: string | null }>,
  title?: string | null,
  sequence?: number | null,
  areaCode?: string | null
): EducationalVideo {
  // 1. Buscar en los bloques si ya vino un enlace válido de YouTube o video directo
  if (blocks && blocks.length > 0) {
    for (const b of blocks) {
      const vidId = extractYouTubeId(b.url)
      if (vidId) {
        return {
          id: vidId,
          title: b.text || title || 'Video didáctico de la lección',
          embedUrl: buildEmbedUrl(vidId),
          watchUrl: `https://www.youtube.com/watch?v=${vidId}`,
          description: 'Observa la explicación didáctica antes de responder.',
          isDirectVideo: false,
        }
      } else if (b.url && (b.type === 'VIDEO' || /\.(mp4|webm|ogg)($|\?)/i.test(b.url) || b.url.includes('/materials/'))) {
        return {
          id: 'custom-video',
          title: b.text || title || 'Video didáctico de la lección',
          embedUrl: b.url,
          watchUrl: b.url,
          description: 'Observa el video compartido por tu profesor.',
          isDirectVideo: true,
        }
      }
    }
  }

  // 2. Mapear por palabras clave específicas del tema
  const t = (title || '').toLowerCase()

  // 2.1. Ciencia y Tecnología
  if (t.includes('sentido') || t.includes('vista') || t.includes('ojo') || t.includes('oído') || t.includes('oido') || t.includes('olfato') || t.includes('gusto') || t.includes('tacto') || t.includes('oreja')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.sentidos
  }
  if (t.includes('cuerpo') || t.includes('salud') || t.includes('hueso') || t.includes('músculo') || t.includes('musculo') || t.includes('higiene') || t.includes('piel')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.cuerpo
  }
  if (t.includes('animal') || t.includes('fauna') || t.includes('vertebrado') || t.includes('ser vivo') || t.includes('seres vivos') || t.includes('biodiversidad') || t.includes('ave')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.animales
  }
  if (t.includes('planta') || t.includes('flora') || t.includes('biohuerto') || t.includes('raíz') || t.includes('semilla') || t.includes('flor') || t.includes('fotosíntesis') || t.includes('hoja')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.plantas
  }
  if (t.includes('ecosistema') || t.includes('agua') || t.includes('río') || t.includes('ambiente') || t.includes('solar') || t.includes('energía') || t.includes('energia') || t.includes('célula') || t.includes('naturaleza')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.ecosistema
  }

  // 2.2. Matemática
  if (t.includes('fracci')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.fracciones
  }
  if (t.includes('multiplic') || t.includes('tabla')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.multiplicacion
  }
  if (t.includes('divis') || t.includes('repart')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.division
  }
  if (t.includes('sum') || t.includes('adici')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.suma
  }
  if (t.includes('rest') || t.includes('sustra')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.resta
  }
  if (t.includes('moneda') || t.includes('compra') || t.includes('mercado') || t.includes('dinero') || t.includes('presupuesto') || t.includes('soles')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.dinero
  }
  if (t.includes('patr') || t.includes('secuencia') || t.includes('quipu')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.patron
  }
  if (t.includes('med') || t.includes('metro') || t.includes('longitud') || t.includes('perímetro') || t.includes('perimetro') || t.includes('balanza')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.longitud
  }
  if (t.includes('forma') || t.includes('figura') || t.includes('geom') || t.includes('ángulo') || t.includes('triángulo') || t.includes('prisma') || t.includes('círculo') || t.includes('cuadrado')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.geometria
  }
  if (t.includes('hora') || t.includes('reloj') || t.includes('tiempo')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.reloj
  }
  if (t.includes('cont') || t.includes('número') || t.includes('numero') || t.includes('1 al 10') || t.includes('1 al 20') || t.includes('1 al 100')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.conteo
  }

  // 2.3. Comunicación
  if (t.includes('poema') || t.includes('rima') || t.includes('verso') || t.includes('estrofa') || t.includes('canción')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.poemas
  }
  if (t.includes('zorro') || t.includes('cuy') || t.includes('fábula') || t.includes('fabula') || t.includes('leyenda') || t.includes('mito') || t.includes('relato') || t.includes('cuento')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.cuento
  }
  if (t.includes('vocal') || t.includes('palabra') || t.includes('pregón') || t.includes('texto') || t.includes('noticia') || t.includes('afiche') || t.includes('ensayo') || t.includes('discurso') || t.includes('lectura') || t.includes('escrib') || t.includes('comprensión')) {
    return VERIFIED_EDUCATIONAL_VIDEOS.lectura
  }

  // 3. Mapear por área curricular como respaldo temático coherente
  const area = (areaCode || '').toUpperCase()
  if (area.includes('CYT')) return VERIFIED_EDUCATIONAL_VIDEOS.sentidos
  if (area.includes('COM')) return VERIFIED_EDUCATIONAL_VIDEOS.cuento
  if (area.includes('MAT')) return VERIFIED_EDUCATIONAL_VIDEOS.conteo

  // 4. Mapear por número de secuencia si no hay otra pista
  const seqMap: Record<number, EducationalVideo> = {
    1: VERIFIED_EDUCATIONAL_VIDEOS.conteo,
    2: VERIFIED_EDUCATIONAL_VIDEOS.suma,
    3: VERIFIED_EDUCATIONAL_VIDEOS.resta,
    4: VERIFIED_EDUCATIONAL_VIDEOS.dinero,
    5: VERIFIED_EDUCATIONAL_VIDEOS.patron,
    6: VERIFIED_EDUCATIONAL_VIDEOS.longitud,
    7: VERIFIED_EDUCATIONAL_VIDEOS.geometria,
    8: VERIFIED_EDUCATIONAL_VIDEOS.reloj,
    9: VERIFIED_EDUCATIONAL_VIDEOS.conteo,
    10: VERIFIED_EDUCATIONAL_VIDEOS.suma,
  }

  if (sequence && seqMap[sequence]) {
    return seqMap[sequence]
  }

  // Fallback por defecto según área
  return VERIFIED_EDUCATIONAL_VIDEOS.conteo
}
