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
import org.rogach.scallop.{ScallopConf, ScallopOption => Opt}
import sttp.client3.UriContext

object Test {
  case class Config(
                   expoId: Long = 0L,
                   shareId: Option[String] = None,
                   )

  def main(args: Array[String]): Unit = {

    object p extends ScallopConf(args) {

      import org.rogach.scallop.*

      printedName = "ReExpo"
      // private val default = Config()

      val expoId: Opt[Long] = opt(required = true,
        descr = "RC exposition identifier",
      )
      val shareId: Opt[String] = opt(
        descr = "Secret RC sharing id if parsing a non-public exposition",
      )

      verify()
      implicit val config: Config = Config(
        expoId  = expoId(),
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

    val uriFinal = uri"https://www.researchcatalogue.net/view/$expoId/${expoId + 1}"

    val cmd: Seq[String] = shareId match {
      case Some(shareIdValue) =>
        val cookieF = File.createTemp(suffix = ".txt")
        val uri1 = uri"https://www.researchcatalogue.net/shared/$shareIdValue"
        val code1 = Seq("curl", "--silent", "--cookie-jar", cookieF.path, uri1.toString).!
        println(s"Code [1]: $code1")
        require(code1 == 0)

        val uri2 = uri"https://www.researchcatalogue.net/profile/show-exposition?exposition=$expoId"
        val code2 = Seq("curl", "--silent", "--cookie", cookieF.path, uri2.toString).!
        println(s"Code [2]: $code2")
        require(code2 == 0)

        Seq("curl", "--silent", "--cookie", cookieF.path, uriFinal.toString)

      case None =>
        Seq("curl", "--silent", uriFinal.toString)
    }
    val res = cmd.!!
    println("\nResult:\n")
    println(res)
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
            uri"https://www.researchcatalogue.net/view/$expoId/${expoId + 1}"
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
