package ru.tinkoff.tcb

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*
import scala.util.Success
import scala.util.Try
import scala.util.matching.Regex

import alleycats.std.map.*
import mouse.ignore
import org.mongodb.scala.bson.*
import org.mongodb.scala.bson.BsonNull
import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.bson.BsonValue

import ru.tinkoff.tcb.bson.BsonEncoder.ops.*
import ru.tinkoff.tcb.bson.optics.BsonOptic

package object bson {
  private type PartialEndo[A]     = PartialFunction[A, A]
  private type PartialEndo2[A, B] = PartialFunction[(A, B), (A, B)]

  /*
    Экстракторы
   */

  object BUndef {
    def unapply(bu: BsonUndefined): true = true
  }

  object BNull {
    def unapply(bn: BsonNull): true = true
  }

  object BBoolean {
    def unapply(bb: BsonBoolean): Some[Boolean] = Some(bb.getValue)
  }

  object BInt {
    def unapply(bi: BsonInt32): Some[Int] = Some(bi.getValue)
  }

  object BLong {
    def unapply(bl: BsonInt64): Some[Long] = Some(bl.getValue)
  }

  object BDouble {
    def unapply(bd: BsonDouble): Some[Double] = Some(bd.getValue)
  }

  object BDecimal {
    def unapply(bd: BsonDecimal128): Some[BigDecimal] = Some(bd.getValue.bigDecimalValue())
  }

  object BString {
    def unapply(bs: BsonString): Some[String] = Some(bs.getValue)
  }

  object BDateTime {
    def unapply(bdt: BsonDateTime): Option[Instant] =
      Try(Instant.ofEpochMilli(bdt.asDateTime().getValue)).toOption
  }

  object BElement {
    def unapply(be: BsonElement): Some[(String, BsonValue)] = Some((be.key, be.value))
  }

  object BArray {
    def unapply(ba: BsonArray): Some[Vector[BsonValue]] = Some(ba.asScala.toVector)
  }

  object BDocument {
    def unapply(bd: BsonDocument): Some[Map[String, BsonValue]] = Some(bd.asScala.toMap)
  }

  object BObjectId {
    def unapply(boi: BsonObjectId): Some[ObjectId] = Some(boi.getValue)
  }

  object BSymbol {
    def unapply(bs: BsonSymbol): Some[String] = Some(bs.getSymbol)
  }

  object BJavaScript {
    def unapply(bjs: BsonJavaScript): Some[String] = Some(bjs.getCode)
  }

  object BScopedJavaScript {
    def unapply(bjs: BsonJavaScriptWithScope): Some[(String, BsonDocument)] =
      Some(bjs.getCode -> bjs.getScope)
  }

  object BRegex {
    def unapply(brx: BsonRegularExpression): Some[(String, String)] =
      Some(brx.getPattern -> brx.getOptions)
  }

  /*
    Расширения
   */

  implicit final class BsonDocumentObjExt(private val doc: BsonDocument.type) extends AnyVal {
    @inline def apply(element: BsonElement): BsonDocument =
      BsonDocument(element.key -> element.value)
  }

  implicit final class BsonValueExt(private val bv: BsonValue) extends AnyVal {
    def getOpt(optic: BsonOptic): Option[BsonValue] = optic.getOpt(bv)

    @inline def decodeAs[T: BsonDecoder]: Try[T]     = BsonDecoder[T].fromBson(bv)
    @inline def decodeOpt[T: BsonDecoder]: Option[T] = decodeAs[T].toOption

    /**
     * Производит слияние двух bson значений
     *
     * bson1 :+ bson2
     *
     * В случае совпадения значений по определённому ключу приоритетными являются значения из bson1
     */
    @inline def :+(other: BsonValue): BsonValue = merge(other, bv, false)

    /**
     * Производит слияние двух bson значений
     *
     * bson1 :+ bson2
     *
     * В случае совпадения значений по определённому ключу приоритетными являются значения из bson2
     */
    @inline def +:(other: BsonValue): BsonValue = merge(other, bv, false)
  }

  implicit final class BsonArrayExt(private val ba: BsonArray) extends AnyVal {
    def modify(f: PartialEndo[BsonValue]): BsonArray = BsonArray.fromIterable(
      ba.asScala.map(f.applyOrElse(_, identity[BsonValue]))
    )

    def modifyAt(idx: Int, f: PartialEndo[BsonValue]): BsonArray = BsonArray.fromIterable(
      ba.asScala.patch(idx, Seq(f.applyOrElse(ba.get(idx), identity[BsonValue])), 1)
    )
  }

  implicit final class BsonDocumentExt(private val bd: BsonDocument) extends AnyVal {
    @inline def getFieldOpt(name: String): Option[BsonValue] =
      if (bd.containsKey(name)) Try(bd.get(name)).toOption else None

    def modify(f: PartialEndo2[String, BsonValue]): BsonDocument =
      BsonDocument(
        bd.asScala.map(f.applyOrElse(_, identity[(String, BsonValue)]))
      )

    def modifyValues(f: PartialEndo[BsonValue]): BsonDocument =
      BsonDocument(
        bd.asScala.view.mapValues(f.applyOrElse(_, identity[BsonValue]))
      )

    /**
     * Add or update
     */
    def +!(el: (String, BsonValue)): BsonDocument =
      if (bd.containsKey(el._1))
        bd.clone().tap { doc =>
          val existing = doc.get(el._1)
          doc.put(el._1, merge(existing, el._2, false))
        }
      else
        bd.clone().append(el._1, el._2)
  }

  implicit val bsonValueBsonDecoder: BsonDecoder[BsonValue] =
    (value: BsonValue) => Success(value)

  implicit val bsonObjectIdBsonDecoder: BsonDecoder[BsonObjectId] =
    (value: BsonValue) => Try(value.asObjectId())

  implicit val booleanBsonDecoder: BsonDecoder[Boolean] =
    (value: BsonValue) => Try(value.asBoolean().getValue)

  implicit val intBsonDecoder: BsonDecoder[Int] =
    (value: BsonValue) => Try(value.asInt32().getValue)

  implicit val longBsongDecoder: BsonDecoder[Long] =
    (value: BsonValue) => Try(value.asInt64().getValue)

  implicit val doubleBsonDecoder: BsonDecoder[Double] =
    (value: BsonValue) => Try(value.asDouble().getValue)

  implicit val bigDecimalBsonDecoder: BsonDecoder[BigDecimal] =
    (value: BsonValue) => Try(value.asDecimal128().getValue.bigDecimalValue())

  implicit val stringBsonDecoder: BsonDecoder[String] =
    (value: BsonValue) => Try(value.asString().getValue)

  implicit val instantBsonDecoder: BsonDecoder[Instant] =
    (value: BsonValue) => Try(Instant.ofEpochMilli(value.asDateTime().getValue))

  implicit val zonedDateTimeBsonDecoder: BsonDecoder[ZonedDateTime] =
    instantBsonDecoder.afterRead(_.atZone(ZoneOffset.UTC))

  implicit val localDateBsonDecoder: BsonDecoder[LocalDate] =
    zonedDateTimeBsonDecoder.afterRead(_.toLocalDate())

  implicit val localDateTimeBsonDecoder: BsonDecoder[LocalDateTime] =
    zonedDateTimeBsonDecoder.afterRead(_.toLocalDateTime())

  implicit val yearBsonDecoder: BsonDecoder[Year] =
    (value: BsonValue) => Try(Year.of(value.asInt32().getValue))

  implicit val uuidBsonDecoder: BsonDecoder[UUID] =
    (value: BsonValue) => Try(UUID.fromString(value.asString().getValue))

  implicit def optionBsonDecoder[T: BsonDecoder]: BsonDecoder[Option[T]] = {
    case _: BsonNull => Success(None)
    case bv          => bv.decodeAs[T].map(Some(_))
  }

  protected def buildBsonDecoder[C[_], T: BsonDecoder](
      builder: => mutable.Builder[T, C[T]]
  ): BsonDecoder[C[T]] =
    BsonDecoder.ofArray(
      _.asScala
        .foldLeft[Try[mutable.Builder[T, C[T]]]](Success(builder))((seq, bv) =>
          seq.flatMap(sqb => BsonDecoder[T].fromBson(bv).map(sqb += _))
        )
        .map(_.result())
    )

  implicit def seqBsonDecoder[T: BsonDecoder]: BsonDecoder[Seq[T]] =
    buildBsonDecoder(Seq.newBuilder[T])

  implicit def listBsonDecoder[T: BsonDecoder]: BsonDecoder[List[T]] =
    buildBsonDecoder(List.newBuilder[T])

  implicit def vectorBsonDecoder[T: BsonDecoder]: BsonDecoder[Vector[T]] =
    buildBsonDecoder(Vector.newBuilder[T])

  implicit def setBsonDecoder[T: BsonDecoder]: BsonDecoder[Set[T]] =
    buildBsonDecoder(Set.newBuilder[T])

  implicit def stringMapBsonDecoder[T: BsonDecoder]: BsonDecoder[Map[String, T]] =
    BsonDecoder.ofDocument(_.asScala.toMap.traverse(BsonDecoder[T].fromBson))

  implicit def mapBsonDecoder[K: BsonKeyDecoder, V: BsonDecoder]: BsonDecoder[Map[K, V]] =
    BsonDecoder.ofDocument(
      _.asScala.toVector
        .traverse { case (key, value) =>
          (BsonKeyDecoder[K].decode(key), BsonDecoder[V].fromBson(value)).mapN((k, v) => (k, v))
        }
        .map(_.toMap)
    )

  implicit def eitherBsonDecoder[L: BsonDecoder, R: BsonDecoder]: BsonDecoder[L Either R] =
    (value: BsonValue) => value.decodeAs[L].map(Left[L, R]).orElse(value.decodeAs[R].map(Right[L, R]))

  implicit def tuple2BsonDecoder[A: BsonDecoder, B: BsonDecoder]: BsonDecoder[(A, B)] =
    BsonDecoder.ofArray { arr =>
      (
        Try(arr.get(0)).flatMap(BsonDecoder[A].fromBson),
        Try(arr.get(1)).flatMap(BsonDecoder[B].fromBson)
      ).mapN((a, b) => (a, b))
    }

  implicit val finiteDurationBsonDecoder: BsonDecoder[FiniteDuration] =
    (value: BsonValue) =>
      Try(value.asString().getValue()).flatMap(str =>
        Try {
          val d = Duration(str)
          FiniteDuration(d._1, d._2)
        }
      )

  implicit val byteArrayBsonDecoder: BsonDecoder[Array[Byte]] =
    (value: BsonValue) => Try(value.asBinary().getData())

  implicit val regexBsonDecoder: BsonDecoder[Regex] =
    (value: BsonValue) => Try(value.asRegularExpression()).map(bre => new Regex(bre.getPattern))

  implicit val bsonValueBsonEncoder: BsonEncoder[BsonValue] =
    (value: BsonValue) => value

  implicit val bsonObjectIdBsonEncoder: BsonEncoder[BsonObjectId] =
    (value: BsonObjectId) => value

  implicit val booleanBsonEncoder: BsonEncoder[Boolean] =
    (value: Boolean) => BsonBoolean(value)

  implicit val intBsonEncoder: BsonEncoder[Int] =
    (value: Int) => BsonInt32(value)

  implicit val longBsonEncoder: BsonEncoder[Long] =
    (value: Long) => BsonInt64(value)

  implicit val doubleBsonEncoder: BsonEncoder[Double] =
    (value: Double) => BsonDouble(value)

  implicit val bigDecimalBsonEncoder: BsonEncoder[BigDecimal] =
    (value: BigDecimal) => BsonDecimal128(value)

  implicit val stringBsonEncoder: BsonEncoder[String] =
    (value: String) => BsonString(value)

  implicit val instantBsonEncoder: BsonEncoder[Instant] =
    (value: Instant) => BsonDateTime(value.toEpochMilli)

  implicit val zonedDateTimeBsonEncoder: BsonEncoder[ZonedDateTime] =
    instantBsonEncoder.beforeWrite(_.toInstant())

  implicit val localDateBsonEncoder: BsonEncoder[LocalDate] =
    zonedDateTimeBsonEncoder.beforeWrite(_.atStartOfDay(ZoneOffset.UTC))

  implicit val localDateTimeBsonEncoder: BsonEncoder[LocalDateTime] =
    zonedDateTimeBsonEncoder.beforeWrite(_.atZone(ZoneOffset.UTC))

  implicit val yearBsonEncoder: BsonEncoder[Year] =
    (value: Year) => BsonInt32(value.getValue)

  implicit val uuidBsonEncoder: BsonEncoder[UUID] =
    (value: UUID) => BsonString(value.toString)

  implicit def optionBsonEncoder[T: BsonEncoder]: BsonEncoder[Option[T]] =
    (value: Option[T]) => value.fold[BsonValue](BsonNull())(_.bson)

  implicit def seqBsonEncoder[T: BsonEncoder]: BsonEncoder[Seq[T]] =
    (value: Seq[T]) => BsonArray.fromIterable(value.map(_.bson))

  implicit def listBsonEncoder[T: BsonEncoder]: BsonEncoder[List[T]] =
    (value: List[T]) => BsonArray.fromIterable(value.map(_.bson))

  implicit def vectorBsonEncoder[T: BsonEncoder]: BsonEncoder[Vector[T]] =
    (value: Vector[T]) => BsonArray.fromIterable(value.map(_.bson))

  implicit def setBsonEncoder[T: BsonEncoder]: BsonEncoder[Set[T]] =
    (value: Set[T]) => BsonArray.fromIterable(value.map(_.bson))

  implicit def stringMapBsonEncoder[T: BsonEncoder]: BsonEncoder[Map[String, T]] =
    (value: Map[String, T]) => BsonDocument(value.view.mapValues(_.bson))

  implicit def mapBsonEncoder[K: BsonKeyEncoder, V: BsonEncoder]: BsonEncoder[Map[K, V]] =
    (value: Map[K, V]) =>
      BsonDocument(value.map { case (key, value) =>
        BsonKeyEncoder[K].encode(key) -> BsonEncoder[V].toBson(value)
      })

  implicit def eitherBsonEncoder[L: BsonEncoder, R: BsonEncoder]: BsonEncoder[L Either R] =
    (value: Either[L, R]) => value.bimap(BsonEncoder[L].toBson, BsonEncoder[R].toBson).merge

  implicit def tuple2BsonEncoder[A: BsonEncoder, B: BsonEncoder]: BsonEncoder[(A, B)] =
    (value: (A, B)) => BsonArray(value._1.bson, value._2.bson)

  implicit val finiteDurationBsonEncoder: BsonEncoder[FiniteDuration] =
    (value: FiniteDuration) => BsonString(value.toString())

  implicit val byteArrayBsonEncoder: BsonEncoder[Array[Byte]] =
    (value: Array[Byte]) => BsonBinary(value)

  implicit val regexBsonEncoder: BsonEncoder[Regex] =
    (value: Regex) => BsonRegularExpression(value)

  protected def merge(
      base: BsonValue,
      patch: BsonValue,
      arraySubvalues: Boolean
  ): BsonValue =
    (base, patch) match {
      case (ld: BsonDocument, rd: BsonDocument) =>
        ld.clone().tap { left =>
          rd.forEach { (key, value) =>
            if (left.containsKey(key))
              ignore(left.put(key, merge(value, left.get(key), false)))
            else
              ignore(left.put(key, value))
          }
        }
      case (baseArr: BsonArray, patchArr: BsonArray) =>
        val mrgPair = (l: BsonValue, r: BsonValue) => merge(l, r, arraySubvalues = true)

        if (baseArr.size >= patchArr.size)
          BsonArray.fromIterable((baseArr.asScala zip patchArr.asScala).map(mrgPair.tupled))
        else
          BsonArray.fromIterable(
            baseArr.asScala
              .zipAll(patchArr.asScala, BsonNull(), patchArr.asScala.last)
              .map(mrgPair.tupled)
          )
      case (p, BNull()) if arraySubvalues => p
      case (_, p)                         => p
    }
}
