/*
 *  WeaveMeta.scala
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

enum WeaveType {
  case Graphical, Block, IFrame
}
case class WeaveMeta(id: Long, tpe: WeaveType, title: String, created: LocalDate)