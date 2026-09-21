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

import org.beangle.commons.lang.Strings
import org.beangle.data.model.Entity
import org.beangle.data.model.meta.Domain
import org.beangle.data.model.util.Populator
import scala.compiletime.uninitialized

/** 导入设置
 */
class ImportSetting {
  var domain: Domain = uninitialized

  var populator: Populator = uninitialized

  var reader: Reader = uninitialized

  var entityClazzes: Map[String, Class[?]] = Map.empty

  var listeners: List[ImportListener] = List.empty[ImportListener]

  var stopOnError: Boolean = true

  var mode: Importer.Mode = Importer.Mode.InsertUpdate

  /** 添加转换监听器
   */
  def addListener(listener: ImportListener): Unit = {
    listeners ::= listener
  }

  /**
   *
   * @param clazz entity class
   * @return a
   */
  def addEntityClazz(clazz: Class[?]): ImportSetting = {
    val name = Strings.uncapitalize(Strings.substringAfterLast(clazz.getName, "."))
    addEntityClazz(clazz, name)
  }

  def addEntityClazz(clazz: Class[?], shortName: String): ImportSetting = {
    require(classOf[Entity[?]].isAssignableFrom(clazz))
    entityClazzes = entityClazzes.updated(shortName, clazz)
    this
  }
}
