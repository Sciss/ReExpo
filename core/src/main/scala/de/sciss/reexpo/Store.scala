/*
 *  Store.scala
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

import de.sciss.file.File
import sttp.model.Uri
//import org.json4s.native.Serialization
//import org.json4s.{CustomSerializer, Formats, JString, ShortTypeHints}

import java.io.FileInputStream
import java.time.{LocalDate, LocalDateTime}

object Store {

  import com.github.plokhotnyuk.jsoniter_scala.macros._
  import com.github.plokhotnyuk.jsoniter_scala.core._

  given uriCodec: JsonValueCodec[Uri] = new JsonValueCodec[Uri] {
    override def decodeValue(in: JsonReader, default: Uri): Uri = Uri(in.readString(""))

    override def encodeValue(x: Uri, out: JsonWriter): Unit = out.writeVal(x.toString)

    override val nullValue: Uri = null.asInstanceOf[Uri]
  }

  given toolCodec   : JsonValueCodec[Tool]      = JsonCodecMaker.make[Tool]
  given toolSeqCodec: JsonValueCodec[Seq[Tool]] = JsonCodecMaker.make[Seq[Tool]]

  //  private val ToolTypeHints = ShortTypeHints(List(
//    classOf[TextTool], classOf[CommentTool]
//  ))
//
//  // N.B. we do _not_ use the formatting the RC uses
//  private val dateSer = new CustomSerializer[LocalDate](_ => ({
//    case js: JString => LocalDate.parse(js.s)
//  }, {
//    case x: LocalDate => JString(x.toString)
//  }))
//
//  // N.B. we do _not_ use the formatting the RC uses
//  private val dateTimeSer = new CustomSerializer[LocalDateTime](_ => ({
//    case js: JString => LocalDateTime.parse(js.s)
//  }, {
//    case x: LocalDateTime => JString(x.toString)
//  }))
//
//  implicit val formats: Formats = Serialization.formats(ToolTypeHints) + dateSer + dateTimeSer

//  def readTool(s: String): Tool = {
//    Serialization.read[Tool](s)
//  }
//
//  def writeWrite(t: Tool): String = {
//    Serialization.write[Tool](t)
//  }

  // UTF-8
  def readTextFile(f: File): String = {
    val fin = new FileInputStream(f)
    try {
      val sz  = fin.available()
      val arr = new Array[Byte](sz)
      fin.read(arr)
      new String(arr, "UTF-8")
    } finally {
      fin.close()
    }
  }
}
