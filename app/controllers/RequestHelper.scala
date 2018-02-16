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

package controllers

import ch.datascience.graph.elements.persisted.PersistedVertex
import ch.datascience.graph.execution.GraphExecutionContext
import ch.datascience.service.utils.persistence.reader.VertexReader
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import play.api.mvc.Controller

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by jeberle on 04.07.17.
 */
trait RequestHelper { this: Controller =>

  def getVertices( id: PersistedVertex#Id )( implicit
      graphExecutionContext: GraphExecutionContext,
                                             graphTraversalSource: GraphTraversalSource, vertexReader: VertexReader
  ): Future[Map[String, PersistedVertex]] = {
    val g = graphTraversalSource
    val t = g.V( Long.box( id ) ).as( "data" ).out( "resource:stored_in" ).as( "bucket" ).select[Vertex]( "data", "bucket" )

    Future.sequence( graphExecutionContext.execute {
      import collection.JavaConverters._
      if ( t.hasNext ) {
        val jmap: Map[String, Vertex] = t.next().asScala.toMap
        for {
          ( key, value ) <- jmap
        } yield for {
          vertex <- vertexReader.read( value )
        } yield key -> vertex
      }
      else
        Seq.empty
    } ).map( _.toMap )
  }

  def getVertex( id: PersistedVertex#Id )( implicit
      graphExecutionContext: GraphExecutionContext,
                                           graphTraversalSource: GraphTraversalSource, vertexReader: VertexReader
  ): Future[Option[PersistedVertex]] = {
    val g = graphTraversalSource
    val t = g.V( Long.box( id ) )

    val future: Future[Option[PersistedVertex]] = graphExecutionContext.execute {
      if ( t.hasNext ) {
        val vertex = t.next()
        vertexReader.read( vertex ).map( Some.apply )
      }
      else
        Future.successful( None )
    }
    future
  }

  def getVertexByType( vtype: String )( implicit
      graphExecutionContext: GraphExecutionContext,
                                        graphTraversalSource: GraphTraversalSource, vertexReader: VertexReader
  ): Future[Option[PersistedVertex]] = {
    val g = graphTraversalSource
    val t = g.V().has( "type", vtype )

    val future: Future[Option[PersistedVertex]] = graphExecutionContext.execute {
      if ( t.hasNext ) {
        val vertex = t.next()
        vertexReader.read( vertex ).map( Some.apply )
      }
      else
        Future.successful( None )
    }
    future
  }

}