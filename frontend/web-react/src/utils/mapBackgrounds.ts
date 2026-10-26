/**
 * Catálogo y gestión de fondos paisajísticos del Perú para el mapa de aprendizaje de RUPI.
 * Permite a estudiantes y docentes personalizar la ambientación geográfica del sendero escolar.
 */

export interface MapBackgroundTheme {
  id: string
  name: string
  region: string
  zone: 'sierra' | 'costa' | 'selva'
  tagline: string
  description: string
  imageUrl: string
  bgColor: string
  gradient: string
  badgeColor: string
}

export const PERUVIAN_BACKGROUNDS: MapBackgroundTheme[] = [
  {
    id: 'ayacucho',
    name: 'Plaza Mayor de Ayacucho',
    region: 'Ayacucho',
    zone: 'sierra',
    tagline: 'Cuna de la Libertad Americana y arte de retablos',
    description: 'La histórica Plaza Mayor de Huamanga con sus arquerías de piedra blanca y sol andino radiante.',
    imageUrl: '/images/ayacucho-plaza.jpg',
    bgColor: '#f7f5ed',
    gradient: 'linear-gradient(180deg, rgba(254, 253, 248, 0.85) 0%, rgba(246, 244, 236, 0.82) 40%, rgba(236, 234, 222, 0.86) 100%)',
    badgeColor: '#173e30',
  },
  {
    id: 'machu-picchu',
    name: 'Machu Picchu y Valle Sagrado',
    region: 'Cusco',
    zone: 'sierra',
    tagline: 'Maravilla del Mundo y santuario de los Incas',
    description: 'Muros milenarios de piedra incaica, andenes verdes y la niebla mística de Huayna Picchu.',
    imageUrl: '/images/backgrounds/machu-picchu.svg',
    bgColor: '#edf5f0',
    gradient: 'linear-gradient(180deg, rgba(253, 255, 253, 0.82) 0%, rgba(242, 249, 244, 0.80) 50%, rgba(230, 242, 234, 0.85) 100%)',
    badgeColor: '#2a5a43',
  },
  {
    id: 'titicaca',
    name: 'Lago Titicaca y Los Uros',
    region: 'Puno',
    zone: 'sierra',
    tagline: 'El lago navegable más alto del mundo',
    description: 'Aguas azules sagradas, islas flotantes de totora y la Cordillera Real en el Altiplano.',
    imageUrl: '/images/backgrounds/titicaca.svg',
    bgColor: '#ebf5fc',
    gradient: 'linear-gradient(180deg, rgba(253, 254, 255, 0.82) 0%, rgba(238, 247, 253, 0.80) 50%, rgba(225, 240, 250, 0.85) 100%)',
    badgeColor: '#1a4c75',
  },
  {
    id: 'pacaya-samiria',
    name: 'Pacaya Samiria y Amazonía',
    region: 'Loreto',
    zone: 'selva',
    tagline: 'Selva de los espejos y delfines rosados',
    description: 'El río Amazonas meandroso, copas de árboles gigantescas y flores flotantes Victoria regia.',
    imageUrl: '/images/backgrounds/pacaya-samiria.svg',
    bgColor: '#edf7f1',
    gradient: 'linear-gradient(180deg, rgba(253, 255, 253, 0.82) 0%, rgba(239, 250, 242, 0.80) 50%, rgba(228, 245, 233, 0.85) 100%)',
    badgeColor: '#1e4b27',
  },
  {
    id: 'huacachina',
    name: 'Oasis de la Huacachina',
    region: 'Ica',
    zone: 'costa',
    tagline: 'El oasis natural de América entre grandes dunas',
    description: 'Altas dunas doradas de arena bañadas por el sol costero y una laguna verde entre palmeras.',
    imageUrl: '/images/backgrounds/huacachina.svg',
    bgColor: '#fdf7ea',
    gradient: 'linear-gradient(180deg, rgba(255, 254, 250, 0.82) 0%, rgba(254, 249, 236, 0.80) 50%, rgba(250, 241, 220, 0.85) 100%)',
    badgeColor: '#b8771f',
  },
  {
    id: 'colca',
    name: 'Cañón del Colca y Volcanes',
    region: 'Arequipa',
    zone: 'sierra',
    tagline: 'El vuelo del cóndor sobre el abismo andino',
    description: 'Paredes profundas de cañón, andenes preíncas, volcanes nevados y el cóndor extendiendo sus alas.',
    imageUrl: '/images/backgrounds/colca.svg',
    bgColor: '#faf0e6',
    gradient: 'linear-gradient(180deg, rgba(255, 252, 250, 0.82) 0%, rgba(252, 244, 236, 0.80) 50%, rgba(246, 233, 220, 0.85) 100%)',
    badgeColor: '#7d432b',
  },
  {
    id: 'nazca',
    name: 'Líneas y Pampas de Nazca',
    region: 'Ica',
    zone: 'costa',
    tagline: 'Enigmas milenarios grabados en el desierto',
    description: 'El colibrí gigante, figuras geométricas y senderos trazados en la pampa bajo el cielo limpio.',
    imageUrl: '/images/backgrounds/nazca.svg',
    bgColor: '#fbf3ea',
    gradient: 'linear-gradient(180deg, rgba(255, 252, 248, 0.82) 0%, rgba(253, 245, 235, 0.80) 50%, rgba(248, 235, 219, 0.85) 100%)',
    badgeColor: '#a14e24',
  },
  {
    id: 'huascaran',
    name: 'Nevado Huascarán y Llanganuco',
    region: 'Áncash',
    zone: 'sierra',
    tagline: 'El techo del Perú y lagunas turquesas',
    description: 'La doble cumbre de nieve perpetua más alta del Perú sobre la hermosa laguna turquesa de Llanganuco.',
    imageUrl: '/images/backgrounds/huascaran.svg',
    bgColor: '#eaf4fa',
    gradient: 'linear-gradient(180deg, rgba(253, 255, 255, 0.82) 0%, rgba(238, 247, 252, 0.80) 50%, rgba(224, 240, 249, 0.85) 100%)',
    badgeColor: '#1b8585',
  },
  {
    id: 'mancora',
    name: 'Playas de Máncora y Costa Norte',
    region: 'Piura',
    zone: 'costa',
    tagline: 'Aguas cálidas, olas y atardeceres dorados',
    description: 'El océano Pacífico norteño, muelles tradicionales, palmeras y el calor playero peruano.',
    imageUrl: '/images/backgrounds/mancora.svg',
    bgColor: '#fdf4eb',
    gradient: 'linear-gradient(180deg, rgba(255, 253, 250, 0.82) 0%, rgba(254, 246, 236, 0.80) 50%, rgba(251, 237, 221, 0.85) 100%)',
    badgeColor: '#f95738',
  },
  {
    id: 'vinicunca',
    name: 'Montaña de 7 Colores (Vinicunca)',
    region: 'Cusco',
    zone: 'sierra',
    tagline: 'Arcoíris mineral esculpido por la naturaleza',
    description: 'Franjas de colores turquesa, oro y magenta en la Cordillera Vilcanota junto a alpacas andinas.',
    imageUrl: '/images/backgrounds/vinicunca.svg',
    bgColor: '#f4eff5',
    gradient: 'linear-gradient(180deg, rgba(255, 253, 255, 0.82) 0%, rgba(248, 242, 249, 0.80) 50%, rgba(241, 230, 243, 0.85) 100%)',
    badgeColor: '#9b4d8a',
  },
  {
    id: 'chanchan',
    name: 'Ciudadela de Chan Chan',
    region: 'La Libertad',
    zone: 'costa',
    tagline: 'La ciudad de barro más grande de América precolombina',
    description: 'Muros de adobe con altorrelieves de peces y pelícanos de la gran cultura Chimú en Trujillo.',
    imageUrl: '/images/backgrounds/chanchan.svg',
    bgColor: '#fbf4ea',
    gradient: 'linear-gradient(180deg, rgba(255, 252, 248, 0.82) 0%, rgba(253, 246, 236, 0.80) 50%, rgba(249, 237, 221, 0.85) 100%)',
    badgeColor: '#a8692c',
  },
  {
    id: 'kuelap',
    name: 'Fortaleza de Kuélap',
    region: 'Amazonas',
    zone: 'selva',
    tagline: 'Muralla monumental de los Guerreros de las Nubes',
    description: 'Casas circulares Chachapoyas con techos cónicos, orquídeas y niebla de ceja de selva.',
    imageUrl: '/images/backgrounds/kuelap.svg',
    bgColor: '#eef5f1',
    gradient: 'linear-gradient(180deg, rgba(253, 255, 254, 0.82) 0%, rgba(241, 249, 244, 0.80) 50%, rgba(230, 243, 235, 0.85) 100%)',
    badgeColor: '#1d4d31',
  },
]

export const DEFAULT_MAP_BACKGROUND = PERUVIAN_BACKGROUNDS[0] // Ayacucho Plaza

const STORAGE_GLOBAL_KEY = 'rupi_active_map_bg'
const BG_CHANGE_EVENT = 'rupi:map-bg-changed'

/**
 * Busca un fondo por su identificador.
 */
export function getMapBackgroundById(id?: string | null): MapBackgroundTheme {
  if (!id) return DEFAULT_MAP_BACKGROUND
  const found = PERUVIAN_BACKGROUNDS.find((bg) => bg.id === id)
  return found || DEFAULT_MAP_BACKGROUND
}

/**
 * Obtiene el fondo activo actual (considerando la ruta específica o la preferencia global).
 */
export function getActiveMapBackground(routeId?: string | null): MapBackgroundTheme {
  try {
    if (routeId) {
      const perRoute = localStorage.getItem(`rupi_map_bg_${routeId}`)
      if (perRoute) {
        return getMapBackgroundById(perRoute)
      }
    }
    const globalId = localStorage.getItem(STORAGE_GLOBAL_KEY)
    if (globalId) {
      return getMapBackgroundById(globalId)
    }
  } catch {
    // Si localStorage no está disponible
  }
  return DEFAULT_MAP_BACKGROUND
}

/**
 * Guarda el fondo seleccionado y notifica a todos los componentes escuchando.
 */
export function setActiveMapBackground(id: string, routeId?: string | null): void {
  try {
    localStorage.setItem(STORAGE_GLOBAL_KEY, id)
    if (routeId) {
      localStorage.setItem(`rupi_map_bg_${routeId}`, id)
    }
    window.dispatchEvent(
      new CustomEvent(BG_CHANGE_EVENT, {
        detail: { id, routeId },
      }),
    )
  } catch (err) {
    console.warn('[MapBackgrounds] No se pudo guardar fondo en localStorage', err)
  }
}

/**
 * Hook para escuchar cambios en el fondo del mapa en tiempo real.
 */
import { useEffect, useState } from 'react'

export function useMapBackground(routeId?: string | null): [MapBackgroundTheme, (id: string) => void] {
  const [currentBg, setCurrentBg] = useState<MapBackgroundTheme>(() => getActiveMapBackground(routeId))

  useEffect(() => {
    setCurrentBg(getActiveMapBackground(routeId))

    const handleCustomChange = (e: Event) => {
      const customEvent = e as CustomEvent<{ id: string; routeId?: string }>
      if (customEvent.detail && customEvent.detail.id) {
        setCurrentBg(getMapBackgroundById(customEvent.detail.id))
      }
    }

    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === STORAGE_GLOBAL_KEY || (routeId && e.key === `rupi_map_bg_${routeId}`)) {
        setCurrentBg(getActiveMapBackground(routeId))
      }
    }

    window.addEventListener(BG_CHANGE_EVENT, handleCustomChange)
    window.addEventListener('storage', handleStorageChange)

    return () => {
      window.removeEventListener(BG_CHANGE_EVENT, handleCustomChange)
      window.removeEventListener('storage', handleStorageChange)
    }
  }, [routeId])

  const setBackground = (id: string) => {
    setActiveMapBackground(id, routeId)
    setCurrentBg(getMapBackgroundById(id))
  }

  return [currentBg, setBackground]
}
