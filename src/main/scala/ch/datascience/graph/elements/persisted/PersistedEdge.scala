package ch.datascience.graph.elements.persisted

import ch.datascience.graph.Constants
import ch.datascience.graph.bases.HasId
import ch.datascience.graph.elements.Edge
import ch.datascience.graph.elements.persisted.impl.ImplPersistedEdge

/**
  * Created by johann on 29/05/17.
  */
trait PersistedEdge
  extends Edge
    with PersistedElement
    with HasId {

  final type Id = Constants.EdgeId

  final type VertexReference = PersistedVertex#Id

  final type PathType = EdgePath[VertexReference, Id]

  final type Prop = PersistedRecordProperty

  final def path: EdgePath[VertexReference, Id] = EdgePath(from, id)

}

object PersistedEdge {

  def apply(
    id: PersistedEdge#Id,
    from: PersistedEdge#VertexReference,
    to: PersistedEdge#VertexReference,
    types: Set[PersistedEdge#TypeId],
    properties: PersistedEdge#Properties
  ): PersistedEdge = ImplPersistedEdge(id, from, to, types, properties)

  def unapply(edge: PersistedEdge): Option[(PersistedEdge#Id, PersistedEdge#VertexReference, PersistedEdge#VertexReference, Set[PersistedEdge#TypeId], PersistedEdge#Properties)] = {
    if (edge eq null)
      None
    else
      Some(edge.id, edge.from, edge.to, edge.types, edge.properties)
  }

}
