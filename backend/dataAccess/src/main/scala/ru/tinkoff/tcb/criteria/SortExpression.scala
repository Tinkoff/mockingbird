package ru.tinkoff.tcb.criteria

import org.mongodb.scala.bson.*

import ru.tinkoff.tcb.bson.BsonEncoder

sealed trait SortDirection
case object Asc extends SortDirection
case object Desc extends SortDirection
object SortDirection {
  def toBSONValue(sd: SortDirection): BsonValue = sd match {
    case Asc  => BsonInt32(1)
    case Desc => BsonInt32(-1)
  }

  implicit val sortDirectionWriter: BsonEncoder[SortDirection] = (value: SortDirection) => toBSONValue(value)
}

final case class SortExpression(name: String, direction: SortDirection)

object SortExpression {
  implicit def toBSONElement(se: SortExpression): BsonElement =
    BsonElement(se.name, SortDirection.toBSONValue(se.direction))

  implicit def toBsonDocument(se: SortExpression): BsonDocument =
    BsonDocument(se.name -> SortDirection.toBSONValue(se.direction))
}
