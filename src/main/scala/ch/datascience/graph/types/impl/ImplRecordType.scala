package ch.datascience.graph.types.impl

import ch.datascience.graph.types.RecordType

/**
  * Created by johann on 12/05/17.
  */
private[types] final case class ImplRecordType[PropKey](properties: Set[PropKey]) extends RecordType[PropKey] {

  override def toString: String = s"RecordType($properties)"

}
