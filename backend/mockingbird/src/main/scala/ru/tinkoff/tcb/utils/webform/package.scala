package ru.tinkoff.tcb.utils

import java.net.URLDecoder

import io.circe.Json

package object webform {
  def decode(s: String): Map[String, List[String]] =
    s.split("&")
      .toList
      .flatMap(kv =>
        kv.split("=", 2) match {
          case Array(k, v) =>
            Some((URLDecoder.decode(k, "UTF-8"), URLDecoder.decode(v, "UTF-8")))
          case _ => None
        }
      )
      .groupMap(_._1)(_._2)

  def toJson(form: Map[String, List[String]]): Json =
    Json.fromFields(form.view.mapValues {
      case List(single) => Json.fromString(single)
      case list         => Json.fromValues(list.map(Json.fromString))
    })
}
