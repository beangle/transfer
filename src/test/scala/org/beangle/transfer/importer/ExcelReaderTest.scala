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
import org.beangle.commons.lang.ClassLoaders
import org.beangle.transfer.Format
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ExcelReaderTest extends AnyFunSpec, Matchers {
  describe("ExcelReader") {
    it("reader") {
      val template = ClassLoaders.getResource("data.xlsx").get
      val reader = new ExcelReader(template.openStream(), 0, Format.Xlsx)
      val title = reader.readAttributes()
      title should not be empty
      title.foreach { attr =>
        attr.name should not be empty
        attr.dataType should not be null
      }
      println(s"attributes: $title")

      val data = reader.read()
      data should not be null
      data.length should be(title.length)
      println(s"data: ${data.toSeq}")

      reader.close()
    }

    it("readAttributes extracts types from comments") {
      val template = ClassLoaders.getResource("data.xlsx").get
      val reader = new ExcelReader(template.openStream(), 0, Format.Xlsx)
      val title = reader.readAttributes()
      title should not be empty
      // 验证至少有一个属性的类型不是默认的 String
      title.exists(_.dataType != DataType.String) should be(true)
      reader.close()
    }

    it("read returns multiple rows") {
      val template = ClassLoaders.getResource("data.xlsx").get
      val reader = new ExcelReader(template.openStream(), 0, Format.Xlsx)
      reader.readAttributes()
      var rowCount = 0
      var row = reader.read()
      while (row != null) {
        rowCount += 1
        row should not be null
        row.length should be > 0
        row = reader.read()
      }
      rowCount should be > 0
      println(s"data rows: $rowCount")
      reader.close()
    }
  }
}
