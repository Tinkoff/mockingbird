package ru.tinkoff.tcb.utils.transformation.json

import java.lang.Boolean as JBoolean
import scala.jdk.CollectionConverters.*

import io.circe.Json
import io.circe.JsonNumber
import io.circe.JsonObject

package object js_eval {
  val circe2js: Json.Folder[AnyRef] = new Json.Folder[AnyRef] {
    override def onNull: AnyRef = null

    override def onBoolean(value: Boolean): AnyRef = JBoolean.valueOf(value)

    override def onNumber(value: JsonNumber): AnyRef = value.toBigDecimal.map(_.bigDecimal).orNull

    override def onString(value: String): AnyRef = value

    override def onArray(value: Vector[Json]): AnyRef = value.map(_.foldWith[AnyRef](this)).asJava

    override def onObject(value: JsonObject): AnyRef =
      value.toMap.view
        .mapValues(_.foldWith[AnyRef](this))
        .toMap
        .asJava
  }
}
