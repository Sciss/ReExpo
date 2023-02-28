/*
 *  ReExpo.scala
 *  (ReExpo)
 *
 *  Copyright (c) 2023 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.reexpo

import de.sciss.log.{Level, Logger}
import org.jsoup.Jsoup
import org.rogach.scallop.{ScallopConf, ScallopOption as Opt}
import sttp.client3.{Request, Response, SimpleHttpClient, UriContext, asStringAlways, basicRequest, emptyRequest}
import sttp.model.headers.CookieWithMeta

import scala.jdk.CollectionConverters.*
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReExpo {
  val log: Logger = Logger("re-expo")

  case class Config(
                     expoId   : Long    = 0L,
                     weaveId  : Long    = 0L,
                     username : String  = "user",
                     password : String  = "pass",
                     debug    : Boolean = false,
                   )

  def main(args: Array[String]): Unit = {

    object p extends ScallopConf(args) {

      import org.rogach.scallop.*

      printedName = "ReExpo"
      private val default = Config()

      val expoId: Opt[Long] = opt(required = true,
        descr = "RC exposition identifier",
        validate = _ > 0L,
      )
      val weaveId: Opt[Long] = opt(default = Some(default.weaveId),
        descr = "RC weave identifier or zero",
        validate = _ >= 0L,
      )
      val user: Opt[String] = opt(required = true,
        descr = "RC account user name",
      )
      val pass: Opt[String] = opt(required = true,
        descr = "RC account password",
      )
      val debug: Opt[Boolean] = toggle(default = Some(default.debug),
        descrYes = "Enable debug logging",
      )

      verify()
      given config: Config = Config(
        expoId    = expoId(),
        weaveId   = weaveId(),
        username  = user(),
        password  = pass(),
        debug     = debug(),
      )
    }
    import p.config
    run()
  }

  def run()(using c: Config): Unit = {
    import c.{weaveId => _, *}
    if c.debug then log.level = Level.Debug
    val re = ReExpo()
    re.login(username = username, password = password)
    try {
      val weaveId = if c.weaveId != 0L then c.weaveId else {
        re.listWeaves(expoId).head.id
      }
      val res = re.listContent(expoId, weaveId)
      println(res.mkString(",\n"))

    } finally {
      re.logout()
    }
  }

  private val baseUri = uri"https://www.researchcatalogue.net"

  // e.g. "30/01/2023"
  private val slashDateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US)
  // e.g. "30.01.2023 - 15:29:37"
  private val modifiedDateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy' - 'HH:mm:ss", Locale.US)

  def parseDate(s: String): LocalDate =
    LocalDate.parse(s, slashDateFmt)

  def parseDateTime(s: String): LocalDateTime =
    if s.isEmpty then LocalDateTime.MIN else LocalDateTime.parse(s, modifiedDateFmt)

  def parseContent(s: String): Seq[Tool] = {
    val doc = Jsoup.parseBodyFragment(s)
    val divTools = doc.select("div.tool").asScala.toList
    divTools.flatMap { divTool =>
      // data-id="1927174"
      // data-title="picture #2"
      // data-date="31/01/2023"
      // data-rotate="0"
      // data-tool="picture"

      val toolId = divTool.attr("data-id").toLong
      val name = divTool.attr("data-title")
      val created = parseDate(divTool.attr("data-date"))
      // XXX TODO: we should use a proper CSS parser; Jsoup doesn't support this out of the box
      val css = mkCssMap(divTool.attr("style"))
      val layer = css("z-index").toInt
      val locked = divTool.attr("data-locked").toInt != 0
      val common = ToolCommon(id = toolId, name = name, created = created, layer = layer, locked = locked)

      val bounds = Rect2D(
        x = cssPx(css("left")),
        y = cssPx(css("top")),
        width = cssPx(css("width")),
        height = cssPx(css("height"))
      )
      val rotation = divTool.attr("data-rotate").toDouble // Option.getOrElse(0.0)
      val style = ToolStyle(bounds = bounds, rotation = rotation)
      val toolTpe = divTool.attr("data-tool")
      val divContent = divTool.select("div.tool-content")
      val fTool: PartialFunction[String, Tool] = {
        case "text" =>
          val author = divTool.attr("data-text-author")
          val modified = parseDateTime(divTool.attr("data-text-modified"))
          val body = divContent.select("span.html-text-editor-content")
          val content = HtmlContent(body.html())
          TextTool(common, style, content, author = author, modified = modified)

        case "simpletext" =>
          val body = divContent.select("span.simple-text-editor-content")
          val content = HtmlContent(body.html())
          SimpleTextTool(common, style, content)

//        case "picture" =>
//          val content = Option.empty[ImageContent] // XXX TODO parse rendered page
//          PictureTool(common, style, content)
//
//        case "audio" =>
//          val content = Option.empty[AudioContent] // XXX TODO parse rendered page
//          AudioTool(common, style, content)
//
//        case "video" =>
//          val content = Option.empty[VideoContent] // XXX TODO parse rendered page
//          VideoTool(common, style, content)

        case "comment" =>
          val author = divTool.attr("data-text-author")
          val modified = parseDateTime(divTool.attr("data-text-modified"))
          val body = divContent.select("textarea")
          val content = body.text()
          val resolved = divTool.attr("data-resolution") == "resolved"
          CommentTool(common, style, content, author = author, modified = modified, resolved = resolved)

//        case "shape" =>
//          val body = divContent.select("svg")
//          val content = SvgContent(body.outerHtml())
//          val shapeType = body.attr("data-type") match {
//            case "rect"         => ShapeType.Rect
//            case "circle"       => ShapeType.Circle
//            case "line"         => ShapeType.HLine
//            case "verticalLine" => ShapeType.VLine
//            case "arrowLeft"    => ShapeType.ArrowLeft
//            case "arrowUp"      => ShapeType.ArrowUp
//            case "arrowRight"   => ShapeType.ArrowRight
//            case "arrowDown"    => ShapeType.ArrowDown
//          }
//          ShapeTool(common, style, content, tpe = shapeType)
      }

      val toolOpt = fTool.lift(toolTpe)
      if toolOpt.isEmpty then log.warn(s"listContent: Skipping unknown tool '$toolTpe'")
      toolOpt
    }
  }

  // XXX TODO hackish
  private def mkCssMap(s: String): Map[String, String] =
    s.split(';').iterator.map { pair =>
      val Array(key, value) = pair.split(':')
      (key, value)
    }.toMap

  // XXX TODO hackish
  private def cssPx(s: String): Int = {
    require(s.endsWith("px"))
    s.substring(0, s.length - 2).toInt
  }
}
class ReExpo() {
  import ReExpo.{log, slashDateFmt, modifiedDateFmt, baseUri, parseDate, parseDateTime, parseContent}

  private val client          = SimpleHttpClient()
  private var cookies         = Seq.empty[CookieWithMeta]

  def login(username: String, password: String): Unit = {
    val u   = baseUri.addPath("session", "login")
    val req = emptyRequest.body(Map("username" -> username, "password" -> password)).post(u)
    val res = client.send(req)

    res.body match {
      case Right(text) =>
        if text.strip().isEmpty then {
          cookies = res.unsafeCookies
        } else {
          println("res >>")
          println(res)
          println("<< res")
          throw RCException("login failed")
        }

      case Left(message) =>
        throw RCException(s"POST $u failed with status code ${res.code}: $message")
    }
  }

  def logout(): Unit = {
    get("session" :: "logout" :: Nil)
    cookies = Nil
  }

  def listWeaves(expoId: Long): Seq[WeaveMeta] = {
    val res = post("editor" :: "weaves" :: Nil, data = Map("research" -> expoId.toString))

    /*

    <tr data-id="626663">
        <td>graphical</td>
        <td>Home</td>
        <td>13/05/2019</td>
    </tr>

    */

    val doc   = Jsoup.parseBodyFragment(s"<table>$res")
    val rows  = doc.select("tr").asScala.toList
    rows.map { row =>
      val weaveId = row.attr("data-id").toLong
      row.select("td").asScala.toList match {
        case cellTpe :: cellTitle :: cellDate :: _ =>
          val tpe = cellTpe.text() match {
            case "graphical"  => WeaveType.Graphical
            case "block"      => WeaveType.Block
            case "iframe"     => WeaveType.IFrame
            case other        => throw RCException(s"Unexpected weave type '$other'")
          }
          val title = cellTitle.text()
          val created = parseDate(cellDate.text())
          WeaveMeta(id = weaveId, tpe = tpe, title = title, created = created)

        case _ =>
          throw RCException(s"Unexpected weave data '$row'")
      }
    }
  }

  def listContent(expoId: Long, weaveId: Long): Seq[Tool] = {
    val res = post("editor" :: "content" :: Nil, data = Map("research" -> expoId.toString, "weave" -> weaveId.toString))
    if log.level <= Level.Debug then {
      println("res >>")
      println(res)
      println("<< res")
    }
    parseContent(res)
  }

  private def post(path: Seq[String], data: Map[String, String] = Map.empty /*, files = None, headers = None*/): String = {
    val u   = baseUri.addPath(path)
    val req = emptyRequest.body(data).cookies(cookies).post(u)
    val res = client.send(req)
    res.body match {
      case Right(text) => text
      case Left(message) =>
        throw RCException(s"POST $u failed with status code ${res.code}: $message")
    }
  }

  private def get(path: Seq[String], params: Map[String, String] = Map.empty): String = {
    val u   = baseUri.addPath(path).addParams(params)
    val req = emptyRequest.cookies(cookies).get(u)
    val res = client.send(req)
    res.body match {
      case Right(text) => text
      case Left(message) =>
        throw RCException (s"GET $u failed with status code ${res.code}: $message")
    }
  }
}

case class RCException(message: String) extends Exception(message)
