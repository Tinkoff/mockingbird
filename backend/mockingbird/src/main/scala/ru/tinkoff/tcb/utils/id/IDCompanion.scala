package ru.tinkoff.tcb.utils.id

import io.circe.*
import net.ceedubs.ficus.readers.ValueReader
import shapeless.tag
import shapeless.tag.@@
import sttp.tapir.Schema
import tofu.logging.Loggable
import tofu.optics.Equivalent

import ru.tinkoff.tcb.bson.*
import ru.tinkoff.tcb.generic.PropSubset
import ru.tinkoff.tcb.generic.RootOptionFields

trait IDCompanion[I] {
  type Aux[T] >: I @@ T <: I @@ T

  def apply[T](id: I): I @@ T = tag[T][I](id)

  def unapply(id: I @@ ?): Some[I] = Some(id)

  def equiv[T]: Equivalent[I, I @@ T] = Equivalent(apply[T](_))(_.asInstanceOf[I])

  implicit def encForID[T](implicit ei: Encoder[I]): Encoder[I @@ T] = ei.contramap(identity)
  implicit def decForID[T](implicit di: Decoder[I]): Decoder[I @@ T] = di.map(apply)

  implicit def bencForID[T](implicit iwrt: BsonEncoder[I]): BsonEncoder[I @@ T] =
    iwrt.beforeWrite(identity)
  implicit def brdrForID[T](implicit irdr: BsonDecoder[I]): BsonDecoder[I @@ T] =
    irdr.afterRead(apply)

  implicit def schemaForID[T](implicit st: Schema[I]): Schema[I @@ T] =
    st.as[I @@ T]

  implicit def rofForID[T]: RootOptionFields[I @@ T] =
    RootOptionFields.mk(Set.empty)

  implicit def idSubset[T]: PropSubset[I @@ T, I]    = new PropSubset[I @@ T, I] {}
  implicit def idSubsetRev[T]: PropSubset[I, I @@ T] = new PropSubset[I, I @@ T] {}

  implicit def idLoggable[T](implicit il: Loggable[I]): Loggable[I @@ T] = il.narrow

  implicit def idValueReader[T](implicit vr: ValueReader[I]): ValueReader[I @@ T] = vr.map(apply)
}
