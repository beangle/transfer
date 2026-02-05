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
import org.beangle.data.model.meta.{Domain, EntityType}
import org.beangle.data.model.util.Populator
import org.beangle.data.orm.Jpas
import org.beangle.transfer.{IllegalFormatException, TransferLogger}
import org.beangle.transfer.importer.Importer.Mode.{Insert, Update}

/**
 * EntityImporter interface.
 *
 * @author chaostone
 */
trait EntityImporter extends Importer {
  def domain: Domain

  def populator: Populator

  def aliases: Iterable[String]
}

object DefaultEntityImporter {
  def apply(setting: ImportSetting): DefaultEntityImporter = {
    val config = Importer.Config(setting.reader.format, setting.mode, setting.stopOnError)
    val importer = new DefaultEntityImporter(config)
    importer.reader = setting.reader
    setting.listeners foreach (x => importer.addListener(x))

    importer.domain = setting.domain
    importer.populator = setting.populator
    setting.entityClazzes foreach { case (n, clz) =>
      importer.addEntityClass(n, clz)
    }
    importer
  }
}

/**
 * DefaultEntityImporter
 *
 * @author chaostone
 */
class DefaultEntityImporter(config: Importer.Config) extends AbstractImporter(config), EntityImporter {
  // [alias,entityType]
  protected val entityTypes = new collection.mutable.HashMap[String, EntityType]

  var populator: Populator = _

  var domain: Domain = _

  override protected def init(): Unit = {
    super.init()
    require(null != populator, "Set populator before transfer")
    require(null != domain, "Set domain before transfer")
    require(entityTypes.nonEmpty, "Add entityType before transfer")
  }

  /**
   * 摘取指定前缀的参数
   */
  private def sub(data: collection.Map[String, Any], alias: String): collection.mutable.Map[String, Any] = {
    val prefix = alias + "."
    val newParams = new collection.mutable.HashMap[String, Any]
    for ((key, value) <- data) {
      if (key.indexOf(prefix) == 0) {
        newParams.put(key.substring(prefix.length), value)
      }
    }
    newParams
  }

  /** 转换一个条目 */
  override def transferItem(): Boolean = {
    var modeValidated = true
    for ((name, etype) <- entityTypes if modeValidated) {
      val subDatas = sub(datas, name)
      if (subDatas.nonEmpty) {
        val entity = getEntity(name)
        if (!validateMode(entity)) {
          modeValidated = false
          result.addFailure("该模式下无法导入该行数据")
        } else {
          subDatas foreach { kv =>
            var value = kv._2
            // 处理空字符串并对所有的字符串进行trim
            value match {
              case s: String => value = if Strings.isBlank(s) then null else Strings.trim(s)
              case _ =>
            }
            // 处理null值
            if (null != value) {
              if (value.equals("null")) value = null
              populateValue(entity, etype, kv._1, value)
            }
          }
        }
      }
    }
    modeValidated
  }

  /**
   * Populate single attribute
   */
  protected def populateValue(entity: Entity[_], etype: EntityType, attr: String, value: Any): Unit = {
    // 当有深层次属性,这里和传统的Populate不一样，导入面向的用户，属性可能出现foreigner.name之类的，在正常的form表单中不会出现
    if (Strings.contains(attr, '.')) {
      val parentPath = Strings.substringBeforeLast(attr, ".")
      val propertyType = populator.init(entity, etype, parentPath)
      val property = propertyType._1
      property match {
        case e: Entity[_] =>
          if (e.persisted) {
            populator.populate(entity, etype, parentPath, null)
            populator.init(entity, etype, parentPath)
          }
        case _ =>
      }
    }

    if (!populator.populate(entity, etype, attr, value)) {
      result.addFailure(Importer.description(attributes, attr) + " 数据格式错误", value)
    }
  }

  protected def validateMode(entity: Entity[_]): Boolean = {
    config.mode match {
      case Insert => !Jpas.proxyResolver.isProxy(entity) //插入模式下，如果代理对象，说明已经被接管，保存过
      case Update => entity.persisted
      case _ => true
    }
  }

  def addEntityClass(alias: String, entityClass: Class[_]): Unit = {
    domain.getEntity(entityClass) match {
      case Some(entityType) => entityTypes.put(alias, entityType)
      case None => throw new RuntimeException("cannot find entity type for " + entityClass)
    }
  }

  protected def getEntity(alias: String): Entity[_] = {
    var entity = objs.get(alias).orNull
    if (null == entity) {
      entityTypes.get(alias) match {
        case Some(entityType) =>
          entity = entityType.newInstance()
          objs.put(alias, entity)
        case None =>
          TransferLogger.error(s"Not register entity type for $alias")
          throw new IllegalFormatException("Not register entity type for " + alias, null)
      }
    }
    entity.asInstanceOf[Entity[_]]
  }

  def aliases: Iterable[String] = entityTypes.keys
}
