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
import org.beangle.transfer.TransferLogger

import scala.collection.mutable.ListBuffer

/** 导入的抽象和缺省实现
 *
 * @author chaostone
 */
abstract class AbstractImporter(val config: Importer.Config) extends Importer {
  protected var result: ImportResult = _
  protected val listeners = new ListBuffer[ImportListener]
  protected val mstatus = new Importer.MutableStatus
  protected var objs = new collection.mutable.HashMap[String, AnyRef]

  var reader: Reader = _
  var attributes: Array[Attribute] = _
  var datas: collection.mutable.Map[String, Any] = _

  protected def init(): Unit = {
    setAttributes(reader.readAttributes())
  }

  /** 进行转换
   */
  def transfer(tr: ImportResult): Unit = {
    require(null != reader)
    this.result = tr
    this.result.importer = this

    val transferStartAt = System.currentTimeMillis()
    try {
      init()
      listeners.foreach(l => l.onStart(tr))
      var stopped = false
      while (!stopped && read()) {
        mstatus.index += 1
        mstatus.location = if null != reader then reader.location else "-1"
        try {
          beforeImportItem()
          if (isDataValid) {
            val errors = tr.errors
            for (l <- listeners; if tr.errors == errors) l.onItemStart(tr)
            if (tr.errors == errors) { // 如果转换前已经存在错误,则不进行转换
              if (transferItem()) {
                for (l <- listeners; if tr.errors == errors) l.onItemFinish(tr)
              }
              if tr.errors == errors then mstatus.successed += 1 else mstatus.failed += 1
            }
          }
        } catch {
          case e: Throwable =>
            TransferLogger.error(e.getMessage, e)
            if config.stopOnError then
              stopped = true
              tr.addFailure("导入异常,剩余数据停止导入", e.getMessage)
            else
              tr.addFailure("导入异常", e.getMessage)
            mstatus.failed += 1
        }
      }
      listeners.foreach(l => l.onFinish(tr))
    } catch {
      case e: Throwable => tr.addFailure("导入异常", e.getMessage)
    } finally {
      if (null != reader) reader.close()
    }
    TransferLogger.debug("importer elapse: " + (System.currentTimeMillis() - transferStartAt))
  }

  def addListener(listener: ImportListener): Importer = {
    listeners += listener
    listener.importer = this
    this
  }

  private def read(): Boolean = {
    val data = reader.read().asInstanceOf[Array[_]]
    if (null == data) {
      this.datas = null
      false
    } else {
      datas = new collection.mutable.HashMap[String, Any]
      data.indices foreach { i =>
        val di = data(i)
        di match
          case null => //ignore
          case a: String => if (Strings.isNotBlank(a)) this.datas.put(attributes(i).name, a)
          case _ => this.datas.put(attributes(i).name, di)
      }
      true
    }
  }

  protected def isDataValid: Boolean = {
    this.datas.values exists {
      case tt: String => Strings.isNotBlank(tt)
      case v => null != v
    }
  }

  def setAttributes(attrs: List[Attribute]): Unit = {
    this.attributes = attrs.toArray
  }

  override def status: Importer.Status = {
    mstatus
  }

  protected def beforeImportItem(): Unit = {
    this.objs.clear()
  }

  override def getObj(name: String): Any = {
    objs.get(name).orNull
  }

  override def setObj(name: String, obj: AnyRef): Unit = {
    if (obj == null) {
      objs.remove(name)
    } else {
      objs.put(name, obj)
    }
  }

  def transferItem(): Boolean
}
