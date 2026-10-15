import type { RouteDetail, RouteNode, RouteUnit } from '../../services/learningRoutesApi'

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

  const unitOfNode = new Map<string, RouteUnit>()
  for (const unit of units) {
    if (unit.nodeIds) {
      for (const id of unit.nodeIds) unitOfNode.set(id, unit)
    } else if (unit.nodes) {
      for (const n of unit.nodes) unitOfNode.set(n.id, unit)
    }
  }

  const stateOf = new Map<string, RouteNode['state']>(route.nodes.map(node => [node.id, node.state]))

  let previous: RouteUnit | undefined
  for (const node of route.nodes) {
    const unit = unitOfNode.get(node.id)
    if (unit && unit !== previous) {
      // Nunca renderizar bandas para paradas huérfanas o contenedores ficticios
      if (unit.code === 'OTRAS' || !unit.id || unit.title.toLowerCase().includes('otras paradas')) {
        previous = unit
        continue
      }
      const allIds = unit.nodeIds ?? unit.nodes?.map(n => n.id) ?? []
      const total = unit.totalNodes ?? allIds.length
      const completed = unit.completedNodes ?? allIds.filter(id => stateOf.get(id) === 'COMPLETADO').length
      bands.set(node.id, {
        unit,
        total,
        completed,
      })
    }
    previous = unit
  }

  return bands
}
