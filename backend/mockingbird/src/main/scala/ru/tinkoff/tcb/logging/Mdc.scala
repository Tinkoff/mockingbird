package ru.tinkoff.tcb.logging

import derevo.derive
import io.circe.Json
import tofu.logging.Loggable
import tofu.logging.LoggedValue
import tofu.logging.derivation.loggable

import ru.tinkoff.tcb.utils.map.*

@derive(loggable)
final case class Mdc(
    correlationId: Option[String] = None,
    payload: Option[Map[String, LoggedValue]] = None
) {
  @inline def +?[T: Loggable](kv: (String, Option[T])): Mdc =
    copy(
      payload = Some(payload.getOrElse(Map.empty) +? (kv._1 -> kv._2.map(Loggable[T].loggedValue))).filter(_.nonEmpty)
    )
  @inline def ++(values: Map[String, LoggedValue]): Mdc =
    copy(payload = Some(payload.getOrElse(Map.empty) ++ values).filter(_.nonEmpty))
  @inline def +[T: Loggable](value: (String, T)): Mdc = $plus(value._1 -> Loggable[T].loggedValue(value._2))
  @inline def +(value: (String, LoggedValue)): Mdc    = copy(payload = Some((payload.getOrElse(Map.empty) + value)))

  def setCorrelationId(value: String): Mdc = copy(correlationId = Some(value))
}

object Mdc {
  val empty: Mdc = Mdc()

  def withPayload(kvp: (String, LoggedValue)*): Mdc = Mdc(payload = Some(Map(kvp: _*)))

  def fromJson(json: Json)(implicit lj: Loggable[Json]): Mdc =
    Mdc(payload = json.asObject.map(_.toMap.view.mapValues(Loggable[Json].loggedValue).toMap))
}
