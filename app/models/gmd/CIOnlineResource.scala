/*
 * Copyright (c) 2011-2017 Interfaculty Department of Geoinformatics, University of
 * Salzburg (Z_GIS) & Institute of Geological and Nuclear Sciences Limited (GNS Science)
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
 */

package models.gmd

import java.net.URL

import info.smart.models.owc100.UrlFormat
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.ClassnameLogger

import scala.xml.NodeSeq

/**
  * Trait of the linkage / online resource
  */
sealed trait ResourceType {
  def name: String

  //  override def toString: String = name
}

/**
  *
  * @param _name type name
  */
case class UnknownResourceType(_name: String) extends ResourceType {
  override def name: String = _name
}

/**
  * type of an online resource
  */
object ResourceType {

  case object WEBSITE extends ResourceType {
    val name = "website"
  }

  case object DOWNLOAD extends ResourceType {
    val name = "download"
  }

  case object MAP extends ResourceType {
    val name = "map"
  }

  case object DATA extends ResourceType {
    val name = "data"
  }

  case object METADATA extends ResourceType {
    val name = "metadata"
  }

  def fromString(str: String): ResourceType = str match {
    case "website" => WEBSITE
    case "download" => DOWNLOAD
    case "map" => MAP
    case "data" => DATA
    case "metadata" => METADATA
    case str => UnknownResourceType(str)
  }

  /**
    * JSON reader
    *
    * @see For more explanation on this little magic: [[https://groups.google.com/forum/?fromgroups=#!starred/play-framework/hGrveOkbJ6U]]
    */
  implicit val reads = Reads[ResourceType](r => JsSuccess(ResourceType.fromString(r.as[String])))
  implicit val writes = Writes[ResourceType](r => JsString(r.name))
}

/**
  * Representation of an CI_OnlineResource
  *
  * @see [[https://geo-ide.noaa.gov/wiki/index.php?title=CI_OnlineResource NOAA EDM Wiki - CI_OnlineResource]]
  * @see [[https://geo-ide.noaa.gov/wiki/index.php?title=Online_Resources NOAA EDM Wiki - Online Resources]]
  */
case class CIOnlineResource(linkage: URL,
                            name: Option[String],
                            description: Option[String],
                            protocol: Option[String],
                            resourceType: ResourceType) extends ClassnameLogger {
  // require(!linkage.trim.isEmpty, "LinkageURL was empty")

  override def toString: String = {
    f"""CIOnlineResource(
       |${linkage},
       |${name},
       |${description},
       |${protocol}
       |${resourceType}
     """.stripMargin.replaceAll("\n", " ")
  }
}

object CIOnlineResource extends ClassnameLogger {
  /**
    * JSON writer
    */
  implicit val writes = (
    (JsPath \ "linkage").write[URL](new UrlFormat) and
      (JsPath \ "name").writeNullable[String] and
      (JsPath \ "description").writeNullable[String] and
      (JsPath \ "protocol").writeNullable[String] and
      (JsPath \ "resourceType").write[ResourceType]
    ) (unlift(CIOnlineResource.unapply))

  /**
    * JSON reader
    */
  implicit val reads = (
    (JsPath \ "linkage").read[URL](new UrlFormat) and
      (JsPath \ "name").readNullable[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "protocol").readNullable[String] and
      (JsPath \ "resourceType").read[ResourceType]
    ) (CIOnlineResource.apply _)

  /**
    * parse CIOnlineResource from XML
    *
    * @param nodeSeq that represents a gmd:CI_OnlineResource
    * @param origin  name of the cataloue origin (used to determine the link type in some cases)
    * @see [[https://geo-ide.noaa.gov/wiki/index.php?title=CI_OnlineResource NOAA EDM Wiki - CI_OnlineResource]]
    * @return
    */
  def fromXml(nodeSeq: NodeSeq, origin: String): CIOnlineResource = {
    val linkage = new URL((nodeSeq \ "linkage" \ "URL").text.trim)
    logger.debug(s"Linkage: ${linkage}")

    import utils.StringUtils._
    val name = (nodeSeq \ "name" \ "CharacterString").text.toOption()
    logger.debug(s"Name: ${name}")
    val description = (nodeSeq \ "description" \ "CharacterString").text.toOption()
    logger.debug(s"Description: ${description}")

    val protocol = linkage.toString match {
      // the GNS CSW has a little bit of a stupid way of assigning protocol to the linkages...
      case r"https?:\/\/data.gns.cri.nz\/rgmad\/(?:thumbs|images|layers)\/.*" => "WWW:LINK-1.0-http--download".toOption()
      case _ => (nodeSeq \ "protocol" \ "CharacterString").text.toOption()
    }
    logger.debug(s"Protocol ${protocol}")

    import utils.StringUtils.Regex
    val resourceType = protocol match {
      case Some("WWW:LINK-1.0-http--metadata-URL") => ResourceType.METADATA
      case Some("WWW:LINK-1.0-http--link") => linkage.toString match {
        case r"https?:\/\/data.gns.cri.nz\/rgmad\/(?:thumbs|images|layers)\/.*" => ResourceType.DOWNLOAD
        case _ => ResourceType.WEBSITE
      }
      case Some("WWW:LINK-1.0-http--downloaddata") => ResourceType.DOWNLOAD
      case Some("OGC:WCS-1.1.0-http-get-capabilities") => ResourceType.METADATA
      case _ => linkage.toString match {
        //from here we start some magic by looking at URLs
        case r"https?:\/\/geoportal\.doc\.govt\.nz\/(?i:ArcGIS)\/.*\/MapServer" => ResourceType.METADATA
        case r"https?:\/\/data.linz.govt.nz\/layer\/.*" => ResourceType.MAP
        case _ => ResourceType.WEBSITE
      }
    }
    logger.debug(s"ResourceType: ${resourceType}")

    CIOnlineResource(linkage, name, description, protocol, resourceType)
  }
}
