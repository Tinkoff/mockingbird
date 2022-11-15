package ru.tinkoff.tcb.mockingbird.config

import com.typesafe.config.Config
import enumeratum.*
import net.ceedubs.ficus.readers.ValueReader

trait FicusEnum[T <: EnumEntry] { self: Enum[T] =>
  implicit val vreader: ValueReader[T] =
    (config: Config, path: String) => self.withNameInsensitive(config.getString(path))
}
