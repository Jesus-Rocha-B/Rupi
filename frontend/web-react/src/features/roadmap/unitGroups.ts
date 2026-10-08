<<<<<<< HEAD
import type { RouteDetail, RouteNode, RouteUnit } from '../../services/learningRoutesApi'
=======
import type { RouteDetail, RouteUnit } from '../../services/learningRoutesApi'
>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)

export type UnitBand = {
  unit: RouteUnit
  completed: number
  total: number
}

/**
 * Unidad que encabeza cada parada del mapa (por id de nodo). El orden de las paradas sigue siendo
 * el de la ruta; solo se marca dónde empieza un tramo de otra unidad. Sin agrupación válida
 * devuelve un mapa vacío y el mapa se dibuja como antes.
 */
export function unitBandsByNode(route: RouteDetail): Map<string, UnitBand> {
  const bands = new Map<string, UnitBand>()
  const units = route.curriculum?.units ?? []
  if (!units.length) return bands

<<<<<<< HEAD
  const unitOfNode = new Map<string, RouteUnit>()
  for (const unit of units) for (const id of unit.nodeIds) unitOfNode.set(id, unit)
  const stateOf = new Map<string, RouteNode['state']>(route.nodes.map(node => [node.id, node.state]))

  let previous: RouteUnit | undefined
  for (const node of route.nodes) {
    const unit = unitOfNode.get(node.id)
    if (unit && unit !== previous) {
      bands.set(node.id, {
        unit,
        total: unit.nodeIds.length,
        completed: unit.nodeIds.filter(id => stateOf.get(id) === 'COMPLETADO').length,
      })
    }
    previous = unit
  }
=======
  for (const unit of units) {
    if (unit.nodes && unit.nodes.length > 0) {
      const firstNode = unit.nodes[0]
      bands.set(firstNode.id, {
        unit,
        completed: unit.completedNodes,
        total: unit.totalNodes,
      })
    }
  }

>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)
  return bands
}
