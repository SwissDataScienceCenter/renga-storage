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

import java.io.{FileInputStream, FileNotFoundException, FileOutputStream}
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import play.api.libs.concurrent.ActorSystemProvider
import play.api.libs.concurrent.Execution.defaultContext
import play.api.mvc.{Controller, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

/**
  * Created by johann on 07/07/17.
  */
@Singleton
class LocalFSBackend @Inject()(actorSystemProvider: ActorSystemProvider) extends Controller with Backend {

  def read(request: RequestHeader, bucket: String, name: String): Future[Result] = {
    Future {
      val fullPath = s"$bucket/$name"
      val (from, to) = getRange(request)

      val is = new FileInputStream(fullPath)
      for (n <- from) {
        is.skip(n)
      }

      val dataContent: Source[ByteString, _] = StreamConverters.fromInputStream(() => is)
      val dataContent2: Source[ByteString, _] = (from, to) match {
        case (Some(n), Some(m)) => dataContent.take(m - n)
        case (None, Some(m)) => dataContent.take(m)
        case _ => dataContent
      }

      Ok.chunked(dataContent2)
    }.recover{
      case _: FileNotFoundException | _: SecurityException => NotFound
    }
  }

  def write(req: RequestHeader, bucket: String, name: String, source: Source[ByteString, _]): Result = {
    implicit val actorSystem: ActorSystem  = actorSystemProvider.get
    implicit val mat: ActorMaterializer = ActorMaterializer()

    val fullPath = s"$bucket/$name"
    val os = new FileOutputStream(fullPath)
    val sink = StreamConverters.fromOutputStream(() => os)
    source.runWith(sink)
    Created
  }

  val RangePattern: Regex = """bytes=(\d+)?-(\d+)?.*""".r

  def getRange(request: RequestHeader): (Option[Int], Option[Int]) = {
    val opt = request.headers.get("Range").map {
      case RangePattern(null, to) => (None, Some(to.toInt))
      case RangePattern(from, null) => (Some(from.toInt), None)
      case RangePattern(from, to) => (Some(from.toInt), Some(to.toInt))
      case _ => (None, None)
    }
    opt.getOrElse((None, None))
  }

  private[this] implicit lazy val ex: ExecutionContext = defaultContext

}