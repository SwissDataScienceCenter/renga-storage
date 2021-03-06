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

import javax.inject.{ Inject, Singleton }
import play.api.Configuration
import play.api.inject.{ BindingKey, Injector }

/**
 * Created by johann on 07/07/17.
 */
@Singleton
class Backends @Inject() ( injector: Injector, configuration: Configuration ) {

  val map: Map[String, StorageBackend] = loadBackends

  def getBackend( name: String ): Option[StorageBackend] = map.get( name )

  private[this] def loadBackends: Map[String, StorageBackend] = {
    val it = for {
      conf <- configuration.getOptional[Configuration]( "storage.backend" )
    } yield for {
      name <- conf.subKeys
      if conf.getOptional[Boolean]( s"$name.enabled" ).getOrElse( false )
    } yield {
      val key = BindingKey( classOf[StorageBackend] ).qualifiedWith( name )
      name -> injector.instanceOf( key )
    }

    it.getOrElse( Seq.empty ).toMap
  }

}
