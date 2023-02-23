/*
 *  Tool.scala
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

import sttp.model.Uri

import java.time.{LocalDate, LocalDateTime}

case class Rect2D(x: Int, y: Int, width: Int, height: Int) {
  def right : Int = x + width
  def bottom: Int = y + height
  
  def cx: Double = x + 0.5 * width
  def cy: Double = y + 0.5 * height
}

case class ToolCommon(id: Long, name: String, created: LocalDate, layer: Int, locked: Boolean = false)
case class ToolStyle(bounds: Rect2D, rotation: Double = 0.0)

sealed trait Tool {
  def common: ToolCommon
  def style : ToolStyle
}

case class HtmlContent(source: String)

sealed trait TextToolBase extends Tool {
  def content: HtmlContent
}

sealed trait AuthoredTextTool extends Tool {
  def author: String
  /** `LocalDateTime.MIN` if undefined (comments) */
  def modified: LocalDateTime
}

case class TextTool(common: ToolCommon, style: ToolStyle,
                    content: HtmlContent, author: String, modified: LocalDateTime)
  extends TextToolBase with AuthoredTextTool

case class SimpleTextTool(common: ToolCommon, style: ToolStyle,
                          content: HtmlContent)
  extends TextToolBase

case class ImageContent(width: Int, height: Int, cache: Uri)

// "{"my":"center center","at":"center center","collision":"none"}"
case class PictureTool(common: ToolCommon, style: ToolStyle,
                       content: Option[ImageContent])
  extends Tool

case class VideoContent(width: Int, height: Int, cache: Uri, poster: ImageContent)

case class VideoTool(common: ToolCommon, style: ToolStyle,
                     content: Option[VideoContent])
  extends Tool

case class AudioContent(cache: Uri)

case class AudioTool(common: ToolCommon, style: ToolStyle,
                     content: Option[AudioContent])
  extends Tool

case class CommentTool(common: ToolCommon, style: ToolStyle,
                       content: String, author: String, modified: LocalDateTime, resolved: Boolean)
  extends Tool with AuthoredTextTool

case class SvgContent(source: String)

enum ShapeType {
  case Rect, Circle, HLine, VLine, ArrowLeft, ArrowUp, ArrowRight, ArrowDown
}

case class ShapeTool(common: ToolCommon, style: ToolStyle,
                     content: SvgContent, tpe: ShapeType)
  extends Tool
