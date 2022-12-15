package ru.tinkoff.tcb.protocol

import sttp.model.Header
import sttp.model.Part
import tofu.logging.LogRenderer
import tofu.logging.Loggable
import tofu.logging.derivation.loggable
import tofu.syntax.logRenderer.*

object log {
  implicit val headerLoggable: Loggable[Header] = new Loggable[Header] {
    override def fields[I, V, R, S](a: Header, i: I)(implicit r: LogRenderer[I, V, R, S]): R =
      i.sub("name")(Loggable.stringValue.putValue(a.name, _: V)) |+|
        r.sub("value", i)(Loggable.stringValue.putValue(a.value, _: V))

    override def putValue[I, V, R, S](a: Header, v: V)(implicit r: LogRenderer[I, V, R, S]): S = v.dict(fields(a, _: I))

    override def logShow(a: Header): String = a.toString()
  }

  implicit def partLoggable[T: Loggable]: Loggable[Part[T]] = loggable.instance[Part[T]]
}
