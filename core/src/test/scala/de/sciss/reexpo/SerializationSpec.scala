package de.sciss.reexpo

//import org.json4s.native.Serialization
//import org.json4s.native.Serialization.{read as sRead, write as sWrite}
//import org.json4s.{CustomSerializer, FieldSerializer, Formats, JString, ShortTypeHints}
import com.github.plokhotnyuk.jsoniter_scala.core.{readFromString, writeToString}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{LocalDate, LocalDateTime}

class SerializationSpec extends AnyFlatSpec with Matchers {
  "Tools" should "serializable" in {
    import Store.toolCodec

    val created   = ReExpo.parseDate("24/02/2023")
    val modified  = ReExpo.parseDateTime("30.01.2023 - 15:29:37")
    val in = TextTool(
      ToolCommon(id = 123457890L, name = "tool", created = created, layer = 2, locked = false),
      ToolStyle(Rect2D(100, 103, 106, 109), rotation = -3.0),
      HtmlContent(""),
      author = "Aeon Flux", modified = modified
    )

    val json = writeToString [Tool](in)
    val out  = readFromString[Tool](json)

//    val json  = write(in)
////    println("json >>")
////    println(json)
////    println("<< json")
//    val out   = read(json)

    out shouldBe in
  }
}