/*
 *  Overview.scala
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

package de.sciss.reexpo.ui

import de.sciss.desktop.OptionPane.{Options, Result}
import de.sciss.desktop.{DialogSource, OptionPane, Preferences, PrefsGUI}
import de.sciss.reexpo.{GraphicalWeave, ReExpo, Tool}
import de.sciss.swingplus.GroupPanel.Element
import de.sciss.swingplus.{GroupPanel, Separator}

import java.awt.RenderingHints
import scala.concurrent.Future
import scala.swing.{Component, Dimension, Graphics2D, MainFrame, PasswordField, Swing, TabbedPane}
import scala.util.{Failure, Success}
import scala.math.{ceil, max, Pi}

object Overview {
  def main(args: Array[String]): Unit = {
    Swing.onEDT(run())
  }

  def run(): Unit = {
    import PrefsGUI.*
    val userPrefs       = Preferences.user(classOf[Overview])
    val prefsExpoId     = userPrefs[Int]("expo-id")
    val prefsWeaveId    = userPrefs[Int]("weave-id")
    val prefsUsername   = userPrefs[String]("username")
    val lbExpoId        = label("Exposition id")
    val expoIdDefault   = 1732852
    val ggExpoId        = intField(prefsExpoId , default = expoIdDefault, max = Int.MaxValue)
    val lbWeaveId       = label("Weave (page) id")
    val weaveIdDefault  = 1733584
    val ggWeaveId       = intField(prefsWeaveId, default = weaveIdDefault, max = Int.MaxValue)
    val lbUsername      = label("RC account e-mail")
    val usernameDefault = "aeon@flux.space"
    val ggUsername      = textField(prefsUsername, default = usernameDefault)
    val lbPassword      = label("RC account password")
    val ggPassword      = PasswordField(16)
    sys.props.get("rc-password").orElse(sys.env.get("rc-password")).foreach { pwInit =>
      ggPassword.peer.setText(pwInit)
    }

    val entries = List(
      lbUsername  -> ggUsername,
      lbPassword  -> ggPassword,
      lbExpoId    -> ggExpoId,
      lbWeaveId   -> ggWeaveId,
    )

    def mkBox(entries: List[(Component, Component)]*): Component = {
      val sepPar    = List.newBuilder[Element.Par]
      val labelsPar = List.newBuilder[Element.Par]
      val fieldsPar = List.newBuilder[Element.Par]
      val collSeq   = List.newBuilder[Element.Seq]

      new GroupPanel {
        {
          val eIt = entries.iterator
          while (eIt.hasNext) {
            val pairs = eIt.next()
            pairs.foreach { case (lb, f) =>
              labelsPar += lb
              fieldsPar += f
              collSeq += Par(Baseline)(lb, f)
            }
            if (eIt.hasNext) {
              val sep = Separator()
              sepPar += sep
              collSeq += sep
            }
          }
        }

        horizontal = Par(sepPar.result() :+ Seq(
          Par(GroupPanel.Alignment.Trailing)(labelsPar.result(): _*),
          Par(fieldsPar.result(): _*)): _*
        )
        vertical = Seq(collSeq.result(): _*)
      }
    }

    val box = mkBox(entries)
    val ggInit = if prefsUsername.getOrElse(usernameDefault) == usernameDefault then ggUsername else ggPassword
    val pane = OptionPane.confirmation(message = box, optionType = Options.OkCancel, focus = Some(ggInit))
    pane.title = "Log-in and select weave" // "ReExpo"
    val res: Result.Value = pane.show()
    if res != Result.Ok then sys.exit()

    val re = ReExpo()
    import concurrent.ExecutionContext.Implicits.global
    val username  = prefsUsername.getOrElse(usernameDefault)
    val password  = ggPassword.password.mkString
    val expoId    = prefsExpoId .getOrElse(expoIdDefault  ).toLong
    val weaveId   = prefsWeaveId.getOrElse(weaveIdDefault ).toLong
//    println(s"user '$username' pass '$password'")

    val futContent = Future {
      re.login(username = username, password = password)
      re.listContent(expoId = expoId, weaveId = weaveId)
    } .andThen {
      case _ => re.logout()
    }

    futContent.onComplete {
      case Success(content) =>
//        println(content.mkString(",\n"))
        Swing.onEDT {
          runWith(weaveId, content)
        }

      case Failure(ex) =>
        import DialogSource.Exception
        (ex -> "Content retrieval failed").show(None)
        sys.exit(1)
    }
  }

  def runWith(weaveId: Long, content: Seq[Tool]): Unit = {
    new MainFrame {
      title = s"Weave $weaveId overview"
      contents = Overview(content)
      pack()
      centerOnScreen()
      open()
    }
  }
}

class Overview(content0: Seq[Tool]) extends Component {
  private var _content  = content0
  private var _scale    = 0.2
  private val _pad      = 40

  private def recalcPrefSize(): Unit = {
    val d = new Dimension(400, 400)
    if _content.nonEmpty then {
      _content.foreach { tool =>
        val b = tool.style.bounds
        d.width   = max(d.width , ceil((b.right  + _pad) * _scale).toInt)
        d.height  = max(d.height, ceil((b.bottom + _pad) * _scale).toInt)
      }
    }
    preferredSize = d
  }

  def content: Seq[Tool] = _content
  def content_=(value: Seq[Tool]): Unit = {
    _content = value
    recalcPrefSize()
    repaint()
  }

  override protected def paintComponent(g: Graphics2D): Unit = {
    super.paintComponent(g)

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.scale(_scale, _scale)

    val atOrig = g.getTransform

    _content.foreach { t =>
      val b   = t.style.bounds
      val rot = t.style.rotation
      val hasRot = rot != 0.0
      if hasRot then g.rotate(rot * Pi/180, b.cx, b.cy)
      g.drawRect(b.x, b.y, b.width, b.height)
      if hasRot then g.setTransform(atOrig)
    }
  }

  // init
  recalcPrefSize()
}
