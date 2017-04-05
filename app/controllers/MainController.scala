/*
 * Copyright (c) 2011-2017 Interfaculty Department of Geoinformatics, University of Salzburg (Z_GIS)
 * & Institute of Geological and Nuclear Sciences Limited (GNS Science)
 * in the SMART Aquifer Characterisation (SAC) programme funded by the New Zealand
 * Ministry of Business, Innovation and Employment (MBIE)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import play.api.mvc._


/**
  * Controller that handles every "unspecific" request.
  */
class MainController extends Controller {
  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index: Action[AnyContent] = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  /**
    * cuts off any trailing "/" from the path
    * @param path
    * @return
    */
  def untrail(path: String): Action[AnyContent] = Action {
    MovedPermanently("/".concat(path))
  }

  /**
    * idea is to provide a listing of api usable endpoints with some parameter description,selectable by the fields param
    *
    * the BuildInfo object is generated through sbt-buildinfo plugin, src under target/scala-2.11/src_managed
    * configuration of this in build.sbt buildinfokeys
    *
    * @param fields
    * @return
    */
  def discovery(fields: Option[String]): Action[AnyContent] = Action { request =>
    val appName = utils.BuildInfo.name
    val appVersion = utils.BuildInfo.version
    val buildNumber = utils.BuildInfo.buildNumber
    Ok(Json.obj(
      "appName" -> appName,
      "version" -> s"$appVersion-$buildNumber"
    ))
  }

}
