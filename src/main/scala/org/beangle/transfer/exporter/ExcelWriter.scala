/*
 * Copyright (C) 2005, The Beangle Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.beangle.transfer.exporter

import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.*
import org.beangle.commons.lang.Chars
import org.beangle.doc.excel.stream.StreamingWriter
import org.beangle.transfer.Format

import java.io.OutputStream

/**
 * ExcelItemWriter class.
 *
 * 委托 StreamingExcelWriter 管理 workbook 生命周期、sheet 状态和数据写入，
 * 自身保留 Writer 接口和导出特有的样式逻辑（caption、自动列宽等）。
 *
 * @author chaostone
 */
class ExcelWriter(val outputStream: OutputStream) extends Writer {

  private val streamingWriter = new StreamingWriter()

  private var titles: Array[String] = _

  private var caption: Option[String] = None

  override def close(): Unit = {
    streamingWriter.save(outputStream)
  }

  override def write(obj: Any): Unit = {
    if (streamingWriter.getCurrentRowNum == 0) {
      writeHeader(caption, titles)
    }
    obj match
      case a: Array[Any] => streamingWriter.writeRow(a.toSeq *)
      case it: Iterable[Any] => streamingWriter.writeRow(it.toSeq *)
      case other => streamingWriter.writeRow(other)
  }

  override def writeHeader(caption: Option[String], titles: Array[String]): Unit = {
    this.titles = titles
    this.caption = caption

    caption foreach { c =>
      streamingWriter.writeCaption(c, titles.length, buildCaptionStyle())
    }

    val maxWith = 15 * 2 //max 15 chinese chars
    var h = 0d
    val widths = titles.indices.map { i =>
      val n = Chars.charLength(titles(i))
      val w = Math.min(n, maxWith)
      val r = n * 1.0 / maxWith
      if (r > h) h = r
      w + 4 // 4 is margin
    }
    var height = Math.ceil(h).toInt
    if (height > 8) height = 8
    val rowHeight = (height * 12 * 20).toShort

    streamingWriter.writeHeaders(titles.toSeq, buildTitleStyle(), widths, Some(rowHeight))
    streamingWriter.freezePane(0, streamingWriter.getCurrentRowNum)
  }

  final def format: Format = Format.Xlsx

  protected def buildTitleStyle(): XSSFCellStyle = {
    val style = streamingWriter.createCellStyle()
    style.setAlignment(HorizontalAlignment.CENTER)
    style.setVerticalAlignment(VerticalAlignment.CENTER)
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND)
    style.setWrapText(true)
    style.setFillForegroundColor(getHeaderForegroundColor())
    style
  }

  protected def buildCaptionStyle(): XSSFCellStyle = {
    val style = streamingWriter.createCellStyle()
    style.setAlignment(HorizontalAlignment.CENTER)
    style.setVerticalAlignment(VerticalAlignment.CENTER)
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND)
    style.setFillForegroundColor(getHeaderForegroundColor())
    val font = streamingWriter.createFont()
    font.setBold(true)
    style.setFont(font)
    style
  }

  protected def getHeaderForegroundColor(): XSSFColor = {
    val rgb = Array(221.toByte, 217.toByte, 196.toByte)
    new XSSFColor(rgb, new DefaultIndexedColorMap)
  }

}
