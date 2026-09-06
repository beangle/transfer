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

package org.beangle.transfer.importer

import org.beangle.commons.io.DataType
import org.beangle.commons.lang.Strings
import org.beangle.doc.excel.stream.StreamingReader
import org.beangle.transfer.Format
import org.beangle.transfer.importer.{Attribute, Reader}

import java.io.InputStream

/**
 * Excel的每行一条数据的读取器（基于 StAX 流式读取，低内存）
 *
 * @author chaostone
 */
class ExcelReader(is: InputStream, sheetNum: Int = 0, val format: Format = Format.Xlsx) extends Reader {

  private val streamingReader = new StreamingReader(is, sheetNum)

  private var attrs: List[Attribute] = _
  private var dataTypes: Array[DataType] = _

  override def readAttributes(): List[Attribute] = {
    val comments = streamingReader.comments
    if (comments.isEmpty) {
      attrs = List.empty
      dataTypes = Array.empty
      return attrs
    }

    // 找到注释中最小行号（属性行，1-based）
    val minRow = comments.keys.map(ref => {
      val digits = ref.filter(_.isDigit)
      if (digits.nonEmpty) digits.toInt else Int.MaxValue
    }).min

    // 流式读取到属性行（手动跳过前 minRow-1 行）
    var headerRow: Option[Array[Any]] = None
    var rowsConsumed = 0
    var row = streamingReader.readRow()
    while (row.isDefined && rowsConsumed < minRow - 1) {
      rowsConsumed += 1
      row = streamingReader.readRow()
    }
    headerRow = row
    streamingReader.skipRows = minRow

    headerRow match {
      case Some(row) =>
        attrs = row.zipWithIndex.map { (cellValue, colIdx) =>
          val colLetter = columnLetter(colIdx)
          val cellRef = colLetter + minRow
          val commentText = comments.getOrElse(cellRef, "")
          if (commentText.isEmpty) {
            Attribute(colIdx + 1, cellValueToString(cellValue), DataType.String, cellValueToString(cellValue))
          } else {
            var name = commentText.trim()
            var dataType = DataType.String
            if (name.indexOf(':') > 0) {
              dataType = DataType.valueOf(Strings.substringAfterLast(name, ":"))
              name = Strings.substringBefore(name, ":")
            }
            Attribute(colIdx + 1, name.trim(), dataType, cellValueToString(cellValue))
          }
        }.toList
        dataTypes = attrs.map(_.dataType).toArray
      case None =>
        attrs = List.empty
        dataTypes = Array.empty
    }
    attrs
  }

  override def read(): Array[Any] = {
    if (dataTypes.isEmpty) return null
    streamingReader.readRow(dataTypes).orNull
  }

  private def cellValueToString(value: Any): String = value match {
    case s: String => s
    case null => ""
    case v => v.toString
  }

  private def columnLetter(colIdx: Int): String = {
    val sb = new StringBuilder()
    var n = colIdx
    while (n >= 0) {
      sb.insert(0, ('A' + n % 26).toChar)
      n = n / 26 - 1
    }
    sb.toString()
  }

  override def close(): Unit = {
    streamingReader.close()
  }

  override def location: String = String.valueOf(streamingReader.currentRowNum)
}
