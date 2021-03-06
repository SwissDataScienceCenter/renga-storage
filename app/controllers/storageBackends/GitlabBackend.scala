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

package controllers.storageBackends

import java.net.URL

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.ByteString
import javax.inject.{ Inject, Singleton }
import models.Repository
import play.api.Configuration
import play.api.http.HttpEntity.Strict
import play.api.http.{ HttpChunk, HttpEntity, Writeable }
import play.api.libs.concurrent.ActorSystemProvider
import play.api.libs.json._
import play.api.libs.streams.Accumulator
import play.api.libs.ws._
import play.api.libs.ws.ahc.StreamedResponse
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.implicitConversions

/**
 * Created by johann on 07/07/17.
 */
@Singleton
class GitlabBackend @Inject() (
    config:                Configuration,
    actorSystemProvider:   ActorSystemProvider,
    implicit val ec:       ExecutionContext,
    implicit val wsclient: WSClient
) extends GitBackend {

  val repo_URL: String = config.get[String]( "storage.backend.gitlab.url" )
  val username: String = config.get[String]( "storage.backend.gitlab.username" )
  val pass: String = config.get[String]( "storage.backend.gitlab.pass" )

  implicit def FutureResponse2Result( response: Future[StreamedResponse] )( implicit writeable: Writeable[ByteString] ): Future[Result] = {
    response map {
      response: StreamedResponse =>
        val headers = response.headers map {
          h => ( h._1, h._2.head )
        }
        Result( ResponseHeader( response.status, headers ), HttpEntity.Chunked( response.bodyAsSource.map( c => HttpChunk.Chunk( writeable.transform( c ) ) ), None ) )
    }
  }
  implicit def Response2Result( response: StreamedResponse )( implicit writeable: Writeable[ByteString] ): Result = {
    val headers = response.headers map {
      h => ( h._1, h._2.head )
    }
    Result( ResponseHeader( response.status, headers ), HttpEntity.Chunked( response.bodyAsSource.map( c => HttpChunk.Chunk( writeable.transform( c ) ) ), None ) )
  }

  implicit def StreamResponse2Result( response: Future[WSResponse] ): Future[Result] = {
    response map {
      response =>
        val headers = response.headers map {
          h => ( h._1, h._2.head )
        }
        Result( ResponseHeader( response.status, headers ), Strict( response.bodyAsBytes, None ) )
    }
  }

  def patchHeaders( h: Headers ): Array[( String, String )] = {

    val host = new URL( repo_URL ).getHost
    h.remove( "Authorization" ).replace( ( "Host", host ) ).toSimpleMap.toArray
  }

  def getRefs( request: RequestHeader, url: String, user: String ): Future[Result] = {
    val result = wsclient.url( repo_URL + "/" + username + "/" + url + ".git/info/refs?" + request.rawQueryString ).withHttpHeaders( patchHeaders( request.headers ): _* ).withAuth( username, pass, WSAuthScheme.BASIC ).withRequestTimeout( 10000.millis )
    result.withMethod( "GET" ).stream()
  }

  def upload( req: RequestHeader, url: String, user: String ): Accumulator[ByteString, Result] = {
    implicit val actorSystem: ActorSystem = actorSystemProvider.get
    implicit val mat: ActorMaterializer = ActorMaterializer()
    Accumulator.source[ByteString].mapFuture { source =>
      val client = wsclient.url( repo_URL + "/" + username + "/" + url + ".git/git-upload-pack" + req.rawQueryString ).withHttpHeaders( patchHeaders( req.headers ): _* ).withAuth( username, pass, WSAuthScheme.BASIC ).withRequestTimeout( 10000.millis )
      client.withBody( SourceBody( source ) ).withMethod( "POST" ).stream()
    }
  }

  def receive( req: RequestHeader, url: String, user: String ): Accumulator[ByteString, Result] = {
    implicit val actorSystem: ActorSystem = actorSystemProvider.get
    implicit val mat: ActorMaterializer = ActorMaterializer()
    Accumulator.source[ByteString].mapFuture { source =>
      val client = wsclient.url( repo_URL + "/" + username + "/" + url + ".git/git-receive-pack" + req.rawQueryString ).withHttpHeaders( patchHeaders( req.headers ): _* ).withAuth( username, pass, WSAuthScheme.BASIC ).withRequestTimeout( 10000.millis )
      client.withBody( SourceBody( source ) ).withMethod( "POST" ).stream()
    }
  }

  def createRepo( request: Repository ): Future[Option[String]] = {
    val client = wsclient.url( repo_URL + "/api/v4/projects" ).withHttpHeaders( "Private-Token" -> pass, "Content-Type" -> "application/json" ).withRequestTimeout( 10000.millis )
    client.post( Json.stringify( Json.obj( "path" -> request.path, "description" -> request.description, "lfs_enabled" -> false ) ) ).map(
      result => {
        ( result.json \ "id" ).toOption.map( i => i.toString() )
      }
    )
  }

}
