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

import javax.inject._

import akka.util.ByteString
import controllers.storageBackends.Backends
import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import org.pac4j.play.PlayWebContext
import org.pac4j.play.store.PlaySessionStore
import play.api.PlayException
import play.api.libs.streams._
import play.api.mvc._

import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex
import scala.util.{Failure, Try}


@Singleton
class IOController @Inject()(config: play.api.Configuration, val playSessionStore: PlaySessionStore, backends: Backends) extends Controller {

  private def getProfiles(implicit request: RequestHeader): List[CommonProfile] = {
    val webContext = new PlayWebContext(request, playSessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    val profiles = profileManager.getAll(true)
    asScalaBuffer(profiles).toList
  }

  val RangePattern: Regex = """bytes=(\d+)?-(\d+)?.*""".r


  def read_object = Action.async { implicit request =>
    val profile = getProfiles(request).head
    val bucket = Try(profile.getAttribute("bucket").toString)
    val name = Try(profile.getAttribute("name").toString)
    val scope = Try(profile.getAttribute("scope").toString)
    val backend = Try(profile.getAttribute("backend").toString)

    backends.getBackend(backend.getOrElse("")) match {
      case Some(back) => {
      val res = scope.flatMap(s => if (s.equalsIgnoreCase("storage:read")) name.flatMap(n => bucket.map(b =>
          back.read(request, b, n))) else Failure(new PlayException("Forbidden", "Wrong scope")))
        res.getOrElse(Future(Forbidden("The token is missing the required permissions")))
      }
      case None => Future(BadRequest(s"The backend $backend is not enabled."))
    }
  }

  def write_object = Action(forward()) { request =>
    request.body
  }

  def forward(): BodyParser[Result] = BodyParser { req =>
    Accumulator.source[ByteString].mapFuture { source =>
      Future {
        val profile = getProfiles(req).head
        val bucket = Try(profile.getAttribute("bucket").toString)
        val name = Try(profile.getAttribute("name").toString)
        val scope = Try(profile.getAttribute("scope").toString)
        val backend = Try(profile.getAttribute("backend").toString)

        backends.getBackend(backend.getOrElse("")) match {
          case Some(back) => {
                       val res = scope.flatMap(s =>
              if (s.equalsIgnoreCase("storage:write"))
                for {n <- name; b <- bucket} yield
                  Right(back.write(req, b, n, source))
              else
                Failure(new PlayException("Forbidden", "Wrong scope")))
            res.getOrElse(Right(Forbidden("The token is missing the required permissions")))
          }
          case None => Right(BadRequest(s"The backend $backend is not enabled."))
        }
      }
    }
  }
}