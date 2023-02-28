package de.sciss.reexpo

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromString, writeToString}
import de.sciss.file.*

object TestStore {
  def main(args: Array[String]): Unit = {
    val s     = Store.readTextFile(userHome / "Downloads" / "test-tools.txt")
    val tools = ReExpo.parseContent(s)
    import Store.toolSeqCodec
    type T = Tool
//    val e: Seq[T] = tools.map(t => Right(t)) :+ Left(12345L)
//    val json  = Serialization.writePretty[Seq[T]](e)
//    val json  = Serialization.writePretty[Seq[T]](tools)
    val json = writeToString [Seq[T]](tools)
    println(json)
//    val back = Serialization.read[Seq[T]](json)
//    val back = Serialization.read[T](Serialization.writePretty[T](tools.head))
    val back = readFromString[Seq[T]](json)
    println()
    println("FIRST:")
    println(back.head)
    println("LAST:")
    println(back.last)
  }
}
