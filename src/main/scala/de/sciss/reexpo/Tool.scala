package de.sciss.reexpo

import sttp.model.Uri

import java.time.{LocalDate, LocalDateTime}

case class Rect2D(x: Int, y: Int, width: Int, height: Int)

case class ToolCommon(id: Long, name: String, created: LocalDate, layer: Int)
case class ToolStyle(bounds: Rect2D, rotation: Double = 0.0)

sealed trait Tool {
  def common: ToolCommon
  def style : ToolStyle
}

case class HtmlContent(source: String)

sealed trait TextToolBase extends Tool {
  def content: HtmlContent
}

case class TextTool(common: ToolCommon, style: ToolStyle,
                    content: HtmlContent, author: String, modified: LocalDateTime)
  extends TextToolBase

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
