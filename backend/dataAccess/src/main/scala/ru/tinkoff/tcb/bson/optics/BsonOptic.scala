package ru.tinkoff.tcb.bson.optics

import scala.util.Try

import org.mongodb.scala.bson.*

import ru.tinkoff.tcb.bson.*

final case class BsonOptic private[optics] (private val BsonPath: Seq[Either[Int, String]]) {
  def \(field: String): BsonOptic = new BsonOptic(BsonPath :+ Right(field))
  def \(index: Int): BsonOptic    = new BsonOptic(BsonPath :+ Left(index))

  /**
   * Compose Optics
   */
  def \\(other: BsonOptic): BsonOptic = new BsonOptic(BsonPath ++ other.BsonPath)

  def set(v: BsonValue): (BsonValue) => BsonValue = { bson =>
    if (validate(bson))
      deferModify { case _ => v }.apply(bson)
    else {
      bson +: BsonPath.foldRight(v)((p, b) =>
        p match {
          case Right(f) => BsonDocument(f -> b)
          case Left(i)  => BsonArray.fromIterable(Seq.fill(i)(BsonNull()) :+ b)
        }
      )
    }
  }

  private def fieldOf(name: String): PartialFunction[BsonValue, BsonValue] = {
    case doc: BsonDocument if doc.containsKey(name) => doc.get(name)
  }

  private def modifyField(
      name: String
  )(
      action: PartialFunction[BsonValue, BsonValue]
  ): PartialFunction[(String, BsonValue), (String, BsonValue)] = {
    case (`name`, v) if action.isDefinedAt(v) => (name, action(v))
  }

  private def elementOf(index: Int): PartialFunction[BsonValue, BsonValue] = {
    case arr: BsonArray if arr.size > index => arr.get(index)
  }

  private[bson] def deferExtract[BOut <: BsonValue](
      action: PartialFunction[BsonValue, BOut]
  ): PartialFunction[BsonValue, BOut] =
    BsonPath.foldRight(action)((e, deferred) =>
      e match {
        case Right(f) => fieldOf(f) andThen deferred
        case Left(i)  => elementOf(i) andThen deferred
      }
    )

  private def deferModify(
      action: PartialFunction[BsonValue, BsonValue]
  ): PartialFunction[BsonValue, BsonValue] =
    BsonPath.foldRight(action)((e, deferred) =>
      e match {
        case Right(f) => { case doc: BsonDocument =>
          doc.modify(modifyField(f)(deferred))
        }
        case Left(i) => { case arr: BsonArray =>
          arr.modifyAt(i, deferred)
        }
      }
    )

  private val extractor = deferExtract { case any => any }

  val getOpt: BsonValue => Option[BsonValue] = extractor.lift

  val get: (BsonValue) => BsonValue = getOpt.andThen(_.getOrElse(BsonNull()))

  val validate: BsonValue => Boolean = extractor.isDefinedAt

  def bimapPath[T](index: Int => T, field: String => T): Seq[T] =
    BsonPath.map(_.bimap(index, field).merge)

  lazy val path: String = bimapPath(i => s"[$i]", identity).mkString(".")

  override def toString: String = s"@->$path"
}

object BsonOptic {
  private val Index = """\[(\d+)\]""".r

  def forPath(path: String*): BsonOptic = new BsonOptic(path.map(Right(_)))
  def forIndex(index: Int): BsonOptic   = new BsonOptic(Seq(Left(index)))
  def fromPathString(path: String): BsonOptic =
    new BsonOptic(path.split('.').toSeq.map {
      case Index(i) => Left(i.toInt)
      case s        => Right(s)
    })

  implicit val bsonOpticBsonEncoder: BsonEncoder[BsonOptic] =
    (value: BsonOptic) => BsonString(value.path)

  implicit val bsonOpticBsonDecoder: BsonDecoder[BsonOptic] =
    (value: BsonValue) => Try(value.asString().getValue).flatMap(str => Try(fromPathString(str)))
}
