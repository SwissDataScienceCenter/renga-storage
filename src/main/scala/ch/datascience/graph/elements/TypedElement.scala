package ch.datascience.graph.elements

import language.higherKinds

/**
  * Base trait for elements that have multi-properties which are constrained by types
  *
  * Typed elements can be validated (see package types).
  *
  */
trait TypedElement[TypeId, Key, +Value, +Prop <: Property[Key, Value, Prop]]
  extends HasMultiProperties[Key, Value, Prop] {

  /**
    * Set of type identifiers
    */
  def types: Set[TypeId]

}
