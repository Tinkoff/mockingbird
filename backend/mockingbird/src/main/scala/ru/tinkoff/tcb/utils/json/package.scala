package ru.tinkoff.tcb.utils

import io.circe.Json
import io.circe.JsonNumber
import io.circe.JsonObject

package object json {
  val json2StringFolder: Json.Folder[String] =
    new Json.Folder[String] {
      override val onNull: String = ""

      override def onBoolean(value: Boolean): String = value.toString

      override def onNumber(value: JsonNumber): String = value.toString

      override def onString(value: String): String = value

      override def onArray(value: Vector[Json]): String = value.map(_.foldWith(this)).mkString(",")

      override def onObject(value: JsonObject): String = Json.fromJsonObject(value).noSpaces
    }

  object JObject {
    def unapplySeq(jo: JsonObject): Some[Seq[(String, Json)]] = Some(jo.toList)
  }
}
