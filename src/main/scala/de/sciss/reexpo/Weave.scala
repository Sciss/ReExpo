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

import java.time.LocalDate

object Weave {
  enum Type {
    case Graphical, Block, IFrame
  }
}
case class Weave(id: Long, tpe: Weave.Type, title: String, created: LocalDate)