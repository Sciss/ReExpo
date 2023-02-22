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

import org.jsoup.Jsoup
import sttp.client3.{Request, Response, SimpleHttpClient, UriContext, asStringAlways, basicRequest, emptyRequest}
import sttp.model.headers.CookieWithMeta

import scala.jdk.CollectionConverters.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReExpo {
//  case class Config(
//                     expoId   : Long    = 0L,
//                     username : String  = "user",
//                     password : String  = "pass",
//                   )
//
//  def main(args: Array[String]): Unit = {
//
//  }
}
class ReExpo() {
  private val client        = SimpleHttpClient()
  private val baseUri       = uri"https://www.researchcatalogue.net"
  private val weaveDateFmt  = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US)
  private var cookies       = Seq.empty[CookieWithMeta]

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

  def listWeaves(expoId: Long): Seq[Weave] = {
    val res   = post("editor" :: "weaves" :: Nil, data = Map("research" -> expoId.toString))

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
            case "graphical"  => Weave.Type.Graphical
            case "block"      => Weave.Type.Block
            case "iframe"     => Weave.Type.IFrame
            case other        => throw RCException(s"Unexpected weave type '$other'")
          }
          val title = cellTitle.text()
          val created = LocalDate.parse(cellDate.text(), weaveDateFmt)
          Weave(id = weaveId, tpe = tpe, title = title, created = created)

        case _ =>
          throw RCException(s"Unexpected weave data '$row'")
      }
    }
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

object Weave {
  enum Type {
    case Graphical, Block, IFrame
  }
}
case class Weave(id: Long, tpe: Weave.Type, title: String, created: LocalDate)