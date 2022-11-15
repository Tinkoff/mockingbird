package ru.tinkoff.tcb.criteria

import org.mongodb.scala.bson.*

import ru.tinkoff.tcb.bson.*

final case class UpdateExpression[+S](name: Option[String], element: BsonElement)

object UpdateExpression {
  def apply[S](name: Option[String], pair: (String, BsonValue)): UpdateExpression[S] =
    UpdateExpression(name, (BsonElement.apply _).tupled.apply(pair))

  val empty: UpdateExpression[Nothing] = UpdateExpression(None, "" -> BsonDocument())

  /**
   * The apply method provides functional-style creation syntax for [[tcb.criteria.UpdateExpression]] instances.
   */
  def apply[S](name: String, element: BsonElement): UpdateExpression[S] =
    new UpdateExpression(Some(name), element)

  def apply[S](name: String, pair: (String, BsonValue)): UpdateExpression[S] =
    UpdateExpression(Some(name), (BsonElement.apply _).tupled.apply(pair))

  def collectPatches[S](patches: Iterable[UpdateExpression[S]]): BsonDocument =
    BsonDocument(patches.groupBy(_.name).collect { case (Some(name), updates) =>
      name -> BsonDocument(updates.map(_.element).map(el => el.key -> el.value))
    })

  /// Implicit Conversions
  // implicit object UpdateExpressionWriter extends BSONDocumentWriter[UpdateExpression] {
  // override def writeTry(expr: UpdateExpression): Try[BsonDocument] = Try(toBSONDocument(expr))
  // }

  implicit def toBSONDocument[S](expr: UpdateExpression[S]): BsonDocument =
    expr match {
      case UpdateExpression(Some(name), element) =>
        BsonDocument(name -> BsonDocument(element))

      case UpdateExpression(None, _) =>
        BsonDocument()
    }

  implicit def toBSONElement[S](expr: UpdateExpression[S]): BsonElement =
    expr.element
}
