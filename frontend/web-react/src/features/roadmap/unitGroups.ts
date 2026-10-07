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
  return bands
}
