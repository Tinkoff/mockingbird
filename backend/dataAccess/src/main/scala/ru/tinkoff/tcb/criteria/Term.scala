package ru.tinkoff.tcb.criteria

import scala.language.dynamics

import org.mongodb.scala.bson.*

import ru.tinkoff.tcb.bson.BsonEncoder
import ru.tinkoff.tcb.bson.BsonEncoder.ops.*

final case class Term[S, T](`_term$name`: String) extends Dynamic {

  /**
   * Logical equality.
   */
  def ===[U <: T: BsonEncoder](rhs: U): Expression[S] =
    Expression(`_term$name`, `_term$name` -> rhs.bson)

  /**
   * Logical equality.
   */
  def ==@[U: BsonEncoder](rhs: U)(implicit ev: Option[U] =:= T): Expression[S] =
    Expression(`_term$name`, `_term$name` -> rhs.bson)

  /**
   * Logical inequality: '''$ne'''.
   */
  def <>[U <: T: BsonEncoder](rhs: U): Expression[S] =
    Expression(`_term$name`, "$ne" -> rhs.bson)

  /**
   * Logical inequality: '''$ne'''.
   */
  def =/=[U <: T: BsonEncoder](rhs: U): Expression[S] = <>[U](rhs)

  /**
   * Logical inequality: '''$ne'''.
   */
  def !==[U <: T: BsonEncoder](rhs: U): Expression[S] = <>[U](rhs)

  /**
   * Less-than comparison: '''$lt'''.
   */
  def <[U <: T: BsonEncoder](rhs: U): Expression[S] =
    Expression(`_term$name`, "$lt" -> rhs.bson)

  /**
   * Less-than or equal comparison: '''$lte'''.
   */
  def <=[U <: T: BsonEncoder](rhs: U): Expression[S] =
    Expression(`_term$name`, "$lte" -> rhs.bson)

  /**
   * Greater-than comparison: '''$gt'''.
   */
  def >[U <: T: BsonEncoder](rhs: U): Expression[S] =
    Expression(`_term$name`, "$gt" -> rhs.bson)

  /**
   * Greater-than or equal comparison: '''$gte'''.
   */
  def >=[U <: T: BsonEncoder](rhs: U): Expression[S] =
    Expression(`_term$name`, "$gte" -> rhs.bson)

  /**
   * Field existence: '''$exists'''.
   */
  def exists: Expression[S] = Expression(`_term$name`, "$exists" -> BsonBoolean(true))

  /**
   * Field unexistence
   */
  def notExists: Expression[S] = Expression(`_term$name`, "$exists" -> BsonBoolean(false))

  /**
   * Field value equals one of the '''values''': '''$in'''.
   */
  def in[U <: T: BsonEncoder](values: Iterable[U]): Expression[S] =
    Expression(`_term$name`, "$in" -> BsonArray.fromIterable(values map (_.bson)))

  /**
   * Field value equals either '''head''' or one of the (optional) '''tail''' values: '''$in'''.
   */
  def in[U <: T: BsonEncoder](head: U, tail: U*): Expression[S] =
    Expression(`_term$name`, "$in" -> BsonArray.fromIterable(Seq(head.bson) ++ tail.map(_.bson)))

  /**
   * Not In '''values''': '''$nin'''.
   */
  def nin[U <: T: BsonEncoder](values: Iterable[U]): Expression[S] =
    Expression(`_term$name`, "$nin" -> BsonArray.fromIterable(values map (_.bson)))

  /**
   * Not In '''head''' or one of the (optional) '''tail''' values: '''$nin'''.
   */
  def nin[U <: T: BsonEncoder](head: U, tail: U*): Expression[S] =
    Expression(`_term$name`, "$nin" -> BsonArray.fromIterable(Seq(head.bson) ++ tail.map(_.bson)))

  def regex(value: String, flags: String = ""): Expression[S] =
    Expression(`_term$name`, "$regex" -> BsonRegularExpression(value, flags))

  def set[V <: T: BsonEncoder](newVal: V): UpdateExpression[S] =
    UpdateExpression("$set", `_term$name` -> newVal.bson)

  def setOp[V: BsonEncoder](newVal: Option[V])(implicit ev: Option[V] =:= T): UpdateExpression[S] =
    newVal match {
      case Some(v) => UpdateExpression("$set", `_term$name` -> v.bson)
      case None    => unset
    }

  def unset: UpdateExpression[S] =
    UpdateExpression("$unset", `_term$name` -> BsonString(""))

  def setOnInsert[V <: T: BsonEncoder](newVal: V): UpdateExpression[S] =
    UpdateExpression("$setOnInsert", `_term$name` -> newVal.bson)

  def inc(amount: Int): UpdateExpression[S] =
    UpdateExpression("$inc", `_term$name` -> BsonInt32(amount))

  def mul(factor: Int): UpdateExpression[S] =
    UpdateExpression("$mul", `_term$name` -> BsonInt32(factor))

  def sort(sd: SortDirection): SortExpression =
    SortExpression(`_term$name`, sd)

  def selectDynamic[U](field: String): Term[S, U] = Term[S, U](`_term$name` + "." + field)
}

object Term {

  /// Class Types
  /**
   * The '''CollectionTermOps''' `implicit` provides EDSL functionality to `Seq`
   * [[reactivemongo.extensions.dsl.criteria.Term]]s only.
   */
  implicit final class CollectionTermOps[S, T](private val term: Term[S, Seq[T]]) extends AnyVal {

    def containsAll(values: Iterable[T])(implicit be: BsonEncoder[T]): Expression[S] =
      Expression(term.`_term$name`, "$all" -> BsonArray.fromIterable(values.map(be.toBson)))

    def contains(element: T)(implicit be: BsonEncoder[T]): Expression[S] = containsAll(Seq(element))

    def contains(query: Expression[T]): Expression[S] =
      Expression(term.`_term$name`, "$elemMatch" -> Expression.toBSONDocument(query))

    def ofSize(size: Int): Expression[S] =
      Expression(term.`_term$name`, "$size" -> BsonInt32(size))

    def notOfSize(size: Int): Expression[S] =
      Expression(term.`_term$name`, "$not" -> BsonDocument("$size" -> BsonInt32(size)))

    def size: SizeOp[S, T] = SizeOp[S, T](term)

    /**
     * Checks that array of elements doesn't contain any elements matching condition
     *
     * @param query
     * @param be
     * @return
     */
    def forNone(query: Expression[T])(implicit be: BsonEncoder[T]): Expression[S] =
      Expression(
        term.`_term$name`,
        "$not" -> BsonDocument("$elemMatch" -> Expression.toBSONDocument(query))
      )

    def push(element: T)(implicit be: BsonEncoder[T]): UpdateExpression[S] =
      UpdateExpression("$push", term.`_term$name` -> be.toBson(element))

    def pull(element: T)(implicit be: BsonEncoder[T]): UpdateExpression[S] =
      UpdateExpression("$pull", term.`_term$name` -> be.toBson(element))

    def pull(expression: Expression[T]): UpdateExpression[S] =
      UpdateExpression("$pull", term.`_term$name` -> Expression.toBSONDocument(expression))

    def insert(element: T)(implicit be: BsonEncoder[T]): UpdateExpression[S] =
      UpdateExpression("$addToSet", term.`_term$name` -> be.toBson(element))

    def isEmpty(implicit be: BsonEncoder[Seq[T]]): Expression[S] = term.exists && (term.size === 0)

    def nonEmpty(implicit be: BsonEncoder[Seq[T]]): Expression[S] = term.exists && (term.size !== 0)

  }

  /**
   * The '''SetTermOps''' `implicit` provides EDSL functionality to `Set`
   * [[reactivemongo.extensions.dsl.criteria.Term]]s only.
   */
  implicit final class SetTermOps[S, T](private val term: Term[S, Set[T]]) extends AnyVal {

    def containsAll(values: Iterable[T])(implicit be: BsonEncoder[T]): Expression[S] =
      Expression(term.`_term$name`, "$all" -> BsonArray.fromIterable(values.map(be.toBson)))

    def contains(element: T)(implicit be: BsonEncoder[T]): Expression[S] = containsAll(Seq(element))

    def contains(query: Expression[T]): Expression[S] =
      Expression(term.`_term$name`, "$elemMatch" -> Expression.toBSONDocument(query))

    def ofSize(size: Int): Expression[S] =
      Expression(term.`_term$name`, "$size" -> BsonInt32(size))

    def notOfSize(size: Int): Expression[S] =
      Expression(term.`_term$name`, "$not" -> BsonDocument("$size" -> BsonInt32(size)))

    // def size: SizeOp[S, T] = SizeOp[S, T](term)

    /**
     * Checks that array of elements doesn't contain any elements matching condition
     *
     * @param query
     * @param be
     * @return
     */
    def forNone(query: Expression[T])(implicit be: BsonEncoder[T]): Expression[S] =
      Expression(
        term.`_term$name`,
        "$not" -> BsonDocument("$elemMatch" -> Expression.toBSONDocument(query))
      )

    def push(element: T)(implicit be: BsonEncoder[T]): UpdateExpression[S] =
      UpdateExpression("$push", term.`_term$name` -> be.toBson(element))

    def pull(element: T)(implicit be: BsonEncoder[T]): UpdateExpression[S] =
      UpdateExpression("$pull", term.`_term$name` -> be.toBson(element))

    def pull(expression: Expression[T]): UpdateExpression[S] =
      UpdateExpression("$pull", term.`_term$name` -> Expression.toBSONDocument(expression))

    def addToSet(element: T)(implicit be: BsonEncoder[T]): UpdateExpression[S] =
      UpdateExpression("$addToSet", term.`_term$name` -> be.toBson(element))

    def isEmpty(implicit be: BsonEncoder[Seq[T]]): Expression[S] = term.exists && (term.size === 0)

    def nonEmpty(implicit be: BsonEncoder[Seq[T]]): Expression[S] = term.exists && (term.size !== 0)

  }

  /**
   * The '''StringTermOps''' `implicit` enriches [[reactivemongo.extensions.dsl.criteria.Term]]s for `String`-only
   * operations.
   */
  implicit class StringTermOps[S, T >: String](private val term: Term[S, T]) extends AnyVal {
    def =~(re: (String, RegexModifier)): Expression[S] =
      Expression(term.`_term$name`, "$regex" -> BsonRegularExpression(re._1, re._2.value))

    def =~(re: String): Expression[S] =
      Expression(term.`_term$name`, "$regex" -> BsonRegularExpression(re, ""))

    def !~(re: (String, RegexModifier)): Expression[S] = Expression(
      term.`_term$name`,
      "$not" -> BsonDocument("$regex" -> BsonRegularExpression(re._1, re._2.value))
    )

    def !~(re: String): Expression[S] =
      Expression(
        term.`_term$name`,
        "$not" -> BsonDocument("$regex" -> BsonRegularExpression(re, ""))
      )
  }

  implicit class SymmetricTermOps[T: BsonEncoder](self: T) {

    def ===[S, U >: T](term: Term[S, U]): Expression[S] = term === self
    def !==[S, U >: T](term: Term[S, U]): Expression[S] = term !== self
    def <=[S, U >: T](term: Term[S, U]): Expression[S]  = term >= self
    def >=[S, U >: T](term: Term[S, U]): Expression[S]  = term <= self
    def <[S, U >: T](term: Term[S, U]): Expression[S]   = term > self
    def >[S, U >: T](term: Term[S, U]): Expression[S]   = term < self

  }

  final case class SizeOp[S, T](term: Term[S, Seq[T]]) {
    def ===(size: Int): Expression[S] = term.ofSize(size)
    def !==(size: Int): Expression[S] = term.notOfSize(size)
  }

}

/**
 * '''RegexModifier''' types provide the ability for developers to specify `$regex` modifiers using type-checked Scala
 * types. For example, specifying a `$regex` which ignores case for the `surname` property can be written as:
 *
 * {{{
 *
 * criteria.surname =~ "smith" -> IgnoreCase
 *
 * }}}
 *
 * Multiple modifiers can be combined using the or (`|`) operator, producing an implementation-defined ordering.
 */
sealed trait RegexModifier {

  /**
   * Use the or operator to combine two or more '''RegexModifier'''s into one logical value.
   */
  def |(other: RegexModifier): RegexModifier =
    CombinedRegexModifier(this, other)

  def value: String
}

case class CombinedRegexModifier(lhs: RegexModifier, rhs: RegexModifier) extends RegexModifier {
  override def value: String = lhs.value + rhs.value
}

case object DotMatchesEverything extends RegexModifier {
  override val value: String = "s"
}

case object ExtendedExpressions extends RegexModifier {
  override val value: String = "x"
}

case object IgnoreCase extends RegexModifier {
  override val value: String = "i"
}

case object MultilineMatching extends RegexModifier {
  override val value: String = "m"
}
