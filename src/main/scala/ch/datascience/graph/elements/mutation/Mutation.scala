package ch.datascience.graph.elements.mutation

/**
  * Created by jeberle on 10.05.17.
  */
trait Mutation {

  def operations: Seq[Operation]

}