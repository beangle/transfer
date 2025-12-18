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

import org.beangle.transfer.Format

/**
 * 数据转换接口
 *
 * @author chaostone
 */
trait Importer {
  /** 启动转换 */
  def transfer(tr: ImportResult): Unit

  /** 返回当前正在转换成的对象 */
  def getObj(name: String): Any

  /** 设置当前正在转换的对象 */
  def setObj(name: String, obj: AnyRef): Unit

  /** 当前导入的原始数据 */
  def datas: collection.mutable.Map[String, Any]

  /** 当前状态 */
  def status: Importer.Status

  /** 导入属性 */
  def attributes: Array[Attribute]
}

object Importer {
  enum Mode {
    case InsertUpdate, Insert, Update
  }

  trait Status {
    /** 当前数据位置 */
    def location: String

    /** 查询正在转换的对象的次序号,从1开始 */
    def index: Int

    /** 得到转换过程中失败的个数 */
    def failed: Int

    /** 得到转换过程中成功的个数 */
    def successed: Int
  }

  private[importer] class MutableStatus extends Status {
    var location: String = _
    var index: Int = _
    var failed: Int = _
    var successed: Int = _
  }

  case class Config(format: Format, mode: Importer.Mode, stopOnError: Boolean)

  def description(attributes: Array[Attribute], attr: String): String = {
    attributes.find(_.name == attr) match {
      case None => ""
      case Some(e) => e.description
    }
  }
}
