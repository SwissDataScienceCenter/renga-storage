/*
 * Copyright 2017 - Swiss Data Science Center (SDSC)
 * A partnership between École Polytechnique Fédérale de Lausanne (EPFL) and
 * Eidgenössische Technische Hochschule Zürich (ETHZ).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.datascience.graph.types.persistence.model

import java.util.UUID

import ch.datascience.graph.NamespaceAndName
import ch.datascience.graph.types.persistence.model.relational.RowGraphDomain

/**
  * Created by johann on 15/03/17.
  */
case class GraphDomain(id: UUID, namespace: String) extends AbstractEntity[RowGraphDomain] {
  require(
    NamespaceAndName.namespaceIsValid(namespace),
    s"Invalid namespace: '$namespace' (Pattern: ${NamespaceAndName.namespacePattern})"
  )

  def toRow: RowGraphDomain = RowGraphDomain(id, namespace)

  final override val entityType: EntityType = EntityType.GraphDomain

}

object GraphDomain {

  def make(rowGraphDomain: RowGraphDomain): GraphDomain = GraphDomain(rowGraphDomain.id, rowGraphDomain.namespace)

  def tupled: ((UUID, String)) => GraphDomain = (GraphDomain.apply _).tupled

}
