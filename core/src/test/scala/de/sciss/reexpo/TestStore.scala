package de.sciss.reexpo

import de.sciss.file.*
import org.json4s.native.Serialization

object TestStore {
  def main(args: Array[String]): Unit = {
    val s     = Store.readTextFile(userHome / "Downloads" / "test-tools.txt")
    val tools = ReExpo.parseContent(s)
    import Store.formats
    val json  = Serialization.writePretty[Seq[Tool]](tools)
    println(json)
  }
}
