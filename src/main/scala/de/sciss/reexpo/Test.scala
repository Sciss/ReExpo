/*
 *  Test.scala
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
import org.jsoup.Jsoup
import org.rogach.scallop.{ScallopConf, ScallopOption as Opt}
import sttp.client3.UriContext

object Test {
  case class Config(
                   expoId : Long = 0L,
                   weaveId: Long = 0L,
                   shareId: Option[String] = None,
                   ) {

    def resolvedWeaveId: Long = if weaveId > 0L then weaveId else expoId + 1
  }

  def main(args: Array[String]): Unit = {

    object p extends ScallopConf(args) {

      import org.rogach.scallop.*

      printedName = "ReExpo - Test"
      private val default = Config()

      val expoId: Opt[Long] = opt(required = true,
        descr = "RC exposition identifier",
        validate = _ > 0L,
      )
      val weaveId: Opt[Long] = opt(default = Some(default.weaveId),
        descr = "RC weave identifier or zero",
        validate = _ >= 0L,
      )
      val shareId: Opt[String] = opt(
        descr = "Secret RC sharing id if parsing a non-public exposition",
      )

      verify()
      implicit val config: Config = Config(
        expoId  = expoId(),
        weaveId = weaveId(),
        shareId = shareId.toOption,
      )
    }
    import p.config
    run()
  }

  def run()(using config: Config): Unit = {
    import sys.process.*
    import de.sciss.file.*
    import config.*

    val uriFinal = uri"https://www.researchcatalogue.net/view/$expoId/$resolvedWeaveId"

    val cmd: Seq[String] = shareId match {
      case Some(shareIdValue) =>
        val cookieF = File.createTemp(suffix = ".txt")
        val uri1 = uri"https://www.researchcatalogue.net/shared/$shareIdValue"
        val code1 = Seq("curl", "--silent", "--cookie-jar", cookieF.path, uri1.toString).!
        println(s"Code [1]: $code1")
        require(code1 == 0)

        // we do not need this intermediate step

//        val uri2 = uri"https://www.researchcatalogue.net/profile/show-exposition?exposition=$expoId"
//        val code2 = Seq("curl", "--silent", "--cookie", cookieF.path, uri2.toString).!
//        println(s"Code [2]: $code2")
//        require(code2 == 0)

        Seq("curl", "--silent", "--cookie", cookieF.path, uriFinal.toString)

      case None =>
        Seq("curl", "--silent", uriFinal.toString)
    }
    val res = cmd.!!
//    println("\nResult:\n")
//    println(res)
    val doc = Jsoup.parse(res)
    println(s"Title: ${doc.title()}")

    val sel = doc.select(".tool-text .tool-content")
    println(s"Number of text tools: ${sel.size()}")
    for i <- 0 until sel.size() do {
      val element = sel.get(i)
      val txt0  = element.text().replace('\n', ' ')
      val txt   = if txt0.length <= 60 then txt0 else s"${txt0.take(60)}…"
      println(s"[$i]: $txt")
    }
  }

  // somehow doesn't work
  def runSTTP()(using config: Config): Unit = {
    import sttp.client3.*
    import config.*

    val shareIdValue = shareId.get

    val req1 = basicRequest.get(
      uri"https://www.researchcatalogue.net/shared/$shareIdValue"
    )

    val backend   = HttpClientSyncBackend()
    val res1      = req1.send(backend)

    // response.header(...): Option[String]
    // println(res1.header("Content-Length"))
    println(s"Success [1]? ${res1.isSuccess}")

    // response.body: by default read into an Either[String, String]
    // to indicate failure or success
    //    println(response.body)
    if res1.isSuccess then res1.cookies match {
      case Right(_ /*cookie*/) :: Nil =>
        val req2 = basicRequest.get(
          uri"https://www.researchcatalogue.net/profile/show-exposition?exposition=$expoId"
        ).cookies(res1)

        val res2 = req2.send(backend)
        println(s"Success [2]? ${res2.isSuccess}")
//        println()
//        println(res2.body)

        if res2.isSuccess then {
          val req3 = basicRequest.get(
            uri"https://www.researchcatalogue.net/view/$expoId/$resolvedWeaveId"
          ).cookies(res2)
          val res3 = req3.send(backend)
          println(s"Success [3]? ${res3.isSuccess}")
          println()
          println(res3.body)
        }

      case _ =>
        println("Woop. Unexpected or missing cookies")
    }
  }
}
