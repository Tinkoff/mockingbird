package ru.tinkoff.tcb.criteria

import org.mongodb.scala.bson.*

import ru.tinkoff.tcb.bson.*

final case class Expression[+S](name: Option[String], element: BsonElement) {
  /// Class Imports
  import Expression.* // scalastyle:ignore

  /**
   * The logical negation operator attempts to invert this '''Expression''' by using complimentary operators if
   * possible, falling back to the general-case wrapping in a `$not` operator.
   */
  def unary_! : Expression[S] =
    this match {
      case Expression(Some(term), BElement("$in", vals)) =>
        Expression(term, BsonElement("$nin", vals))

      case Expression(Some(term), BElement("$nin", vals)) =>
        Expression(term, BsonElement("$in", vals))

      case Expression(Some(term), BElement("$ne", vals)) =>
        Expression(term, BsonElement(term, vals))

      case Expression(Some(term), BElement("$exists", BBoolean(value))) =>
        Expression(Some(term), BsonElement("$exists", BsonBoolean(!value)))

      case Expression(Some(term), BElement(field, vals)) if field == term =>
        Expression(term, BsonElement("$ne", vals))

      case Expression(None, BElement("$nor", vals)) =>
        Expression(None, BsonElement("$or", vals))

      case Expression(None, BElement("$or", vals)) =>
        Expression(None, BsonElement("$nor", vals))

      case Expression(Some("$not"), el) =>
        Expression(None, el)

      case Expression(Some(n), _) =>
        Expression(Some("$not"), BsonElement(n, BsonDocument(element)))

      case Expression(None, el) =>
        Expression(Some("$not"), el)
    }

  /**
   * Conjunction: ''AND''.
   */
  def &&[SS >: S](rhs: Expression[SS]): Expression[SS] = combine("$and", rhs)

  /**
   * Negation of conjunction: ''NOR''.
   */
  def !&&[SS >: S](rhs: Expression[SS]): Expression[SS] = combine("$nor", rhs)

  /**
   * Disjunction: ''OR''.
   */
  def ||[SS >: S](rhs: Expression[SS]): Expression[SS] = combine("$or", rhs)

  /**
   * The isEmpty method reports as to whether or not this '''Expression''' has neither a `name` nor an assigned value.
   */
  def isEmpty: Boolean = name.isEmpty && element.key.isEmpty

  private def combine[SS >: S](op: String, rhs: Expression[SS]): Expression[SS] =
    if (rhs.isEmpty)
      this
    else
      (name, element) match {
        case (None, BElement(`op`, arr: BsonArray)) =>
          Expression(
            None,
            BsonElement(op, arr.tap(_.addAll(BsonArray(toBSONDocument(rhs)))))
          )

        case (_, BElement("", _)) => rhs

        case _ =>
          Expression(
            None,
            BsonElement(
              op,
              BsonArray(
                toBSONDocument(this),
                toBSONDocument(rhs)
              )
            )
          )
      }
}

object Expression {

  /**
   * The empty property is provided so that ''monoid'' definitions for '''Expression''' can be easily provided.
   */
  val empty = new Expression[Nothing](None, BsonElement("", BsonDocument()))

  /**
   * The apply method provides functional-style creation syntax for [[tcb.criteria.Expression]] instances.
   */
  def apply[S](name: String, element: BsonElement): Expression[S] =
    new Expression(Some(name), element)

  def apply[S](name: String, pair: (String, BsonValue)): Expression[S] =
    new Expression(Some(name), (BsonElement.apply _).tupled(pair))

  def apply[S](name: Option[String], pair: (String, BsonValue)): Expression[S] =
    new Expression(name, (BsonElement.apply _).tupled(pair))

  /// Implicit Conversions
  implicit def expressionBsonEncoder[S]: BsonEncoder[Expression[S]] =
    (value: Expression[S]) => toBSONDocument(value)

  implicit def toBSONDocument[S](expr: Expression[S]): BsonDocument =
    expr match {
      case Expression(Some(name), BElement(field, element)) if name == field =>
        BsonDocument(field -> element)

      case Expression(Some(name), element) =>
        BsonDocument(name -> BsonDocument(element))

      case Expression(None, BElement("", _)) =>
        BsonDocument()

      case Expression(None, element) =>
        BsonDocument(element)
    }

  implicit def toBSONKeyValue[S](expr: Expression[S]): (String, BsonValue) =
    expr.element.key -> expr.element.value
}
