package ru.tinkoff.tcb.predicatedsl.xml

import javax.xml.xpath.XPathFactory

import cats.data.NonEmptyList
import cats.data.Validated
import cats.data.ValidatedNel
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import kantan.xpath.*
import kantan.xpath.implicits.*
import org.bson.BsonInvalidOperationException
import sttp.tapir.Schema

import ru.tinkoff.tcb.bson.BsonDecoder
import ru.tinkoff.tcb.bson.BsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.generic.RootOptionFields
import ru.tinkoff.tcb.instances.predicate.and.*
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.predicatedsl.Keyword.*
import ru.tinkoff.tcb.predicatedsl.PredicateConstructionError
import ru.tinkoff.tcb.predicatedsl.SpecificationError
import ru.tinkoff.tcb.predicatedsl.json.JsonPredicate
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.protocol.xml.*
import ru.tinkoff.tcb.utils.circe.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.json.JObject
import ru.tinkoff.tcb.xpath.*

abstract class XmlPredicate extends (Node => Boolean) {
  def definition: XmlPredicate.Spec
  override def apply(xml: Node): Boolean

  override def hashCode(): Int = definition.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case jp: XmlPredicate => jp.definition == definition
    case _                => false
  }
}

object XmlPredicate {
  type Spec = Map[Xpath, Map[Keyword.Xml, Json]]

  implicit val xmlPredicateDecoder: Decoder[XmlPredicate] =
    Decoder[Spec].emap(apply(_).toEither.leftMap(_.toList.mkString(", ")))

  implicit val xmlPredicateEncoder: Encoder[XmlPredicate] =
    Encoder[Spec].contramap(_.definition)

  implicit val xmlPredicateSchema: Schema[XmlPredicate] =
    implicitly[Schema[Spec]].as[XmlPredicate]

  implicit val xmlPredicateBsonDecoder: BsonDecoder[XmlPredicate] =
    BsonDecoder[Spec].afterReadTry(
      apply(_).toEither.leftMap(errs => new BsonInvalidOperationException(errs.toList.mkString(", "))).toTry
    )

  implicit val xmlPredicateBsonEncoder: BsonEncoder[XmlPredicate] =
    BsonEncoder[Spec].beforeWrite(_.definition)

  implicit val xmlPredicateRootOptionFields: RootOptionFields[XmlPredicate] =
    RootOptionFields.mk[XmlPredicate](RootOptionFields[Spec].fields, RootOptionFields[Spec].isOptionItself)

  /**
   * @param description
   *   Имеет вид: {"/xpath": <predicate description>]
   * @return
   */
  def apply(
      description: Spec
  ): ValidatedNel[PredicateConstructionError, XmlPredicate] =
    description.toVector
      .map { case (xPath, spec) =>
        spec.toVector
          .map(mkPredicate.tupled)
          .reduceOption(_ |+| _)
          .getOrElse(Validated.valid((_: Option[Node]) => true))
          .leftMap(errors => NonEmptyList.one(SpecificationError(xPath.raw, errors)))
          .map(pred => (n: Node) => n.evalXPath[Node](xPath.toXPathExpr).toOption pipe pred)
      }
      .reduceOption(_ |+| _)
      .getOrElse(Validated.valid((_: Node) => true))
      .map(f =>
        new XmlPredicate {
          override def definition: Map[Xpath, Map[Keyword.Xml, Json]] = description

          override def apply(xml: Node): Boolean = f(xml)
        }
      )

  private val xPathFactory = XPathFactory.newInstance()

  private val mkPredicate: (Keyword.Xml, Json) => ValidatedNel[(Keyword, Json), Option[Node] => Boolean] =
    (kwd, jv) =>
      (kwd, jv) match {
        case (Equals, JsonString(str)) =>
          Validated.valid(_.exists(_.evalXPath[String](unsafeXPathExpr(s"self::node()[text()='$str']")).isRight))
        case (Equals, JsonNumber(jnum)) =>
          Validated.valid(
            _.exists(_.evalXPath[String](unsafeXPathExpr(s"self::node()[number(text())=$jnum]")).isRight)
          )
        case (NotEq, JsonString(str)) =>
          Validated.valid(_.exists(_.evalXPath[String](unsafeXPathExpr(s"self::node()[not(text()='$str')]")).isRight))
        case (NotEq, JsonNumber(jnum)) =>
          Validated.valid(
            _.exists(_.evalXPath[String](unsafeXPathExpr(s"self::node()[not(number(text())=$jnum)]")).isRight)
          )
        case (Greater, JsonNumber(jnum)) =>
          Validated.valid(
            _.exists(_.evalXPath[String](unsafeXPathExpr(s"self::node()[number(text())>$jnum]")).isRight)
          )
        case (Gte, JsonNumber(jnum)) =>
          Validated.valid(
            _.exists(_.evalXPath[String](unsafeXPathExpr(s"self::node()[number(text())>=$jnum]")).isRight)
          )
        case (Less, JsonNumber(jnum)) =>
          Validated.valid(
            _.exists(_.evalXPath[String](unsafeXPathExpr(s"self::node()[number(text())<$jnum]")).isRight)
          )
        case (Lte, JsonNumber(jnum)) =>
          Validated.valid(
            _.exists(_.evalXPath[String](unsafeXPathExpr(s"self::node()[number(text())<=$jnum]")).isRight)
          )
        case (Rx, JsonString(str)) =>
          Validated.valid(_.exists(_.evalXPath[String](xp"text()").exists(_.matches(str))))
        case (Size, JsonNumber(jnum)) if jnum.toInt.isDefined =>
          Validated.valid(
            _.exists(_.evalXPath[String](unsafeXPathExpr(s"self::node()[string-length(text())=$jnum]")).isRight)
          )
        case (Exists, JsonBoolean(true))  => Validated.valid(_.isDefined)
        case (Exists, JsonBoolean(false)) => Validated.valid(_.isEmpty)
        case (Cdata, JsonDocument(JObject("==" -> JsonString(value)))) =>
          Validated.valid(
            _.exists(_.evalXPath[String](xp"self::node()").contains(value))
          )
        case (Cdata, JsonDocument(JObject("~=" -> JsonString(value)))) =>
          Validated.valid(
            _.exists(_.evalXPath[String](xp"self::node()").exists(_.matches(value)))
          )
        case (JCdata, spec) =>
          Validated
            .fromEither(for {
              jspec <- spec
                .as[Map[JsonOptic, Map[Keyword.Json, Json]]]
                .leftMap(_ => (kwd.asInstanceOf[Keyword] -> spec))
              jpred <- JsonPredicate(jspec).toEither.leftMap(errs =>
                (kwd.asInstanceOf[Keyword] -> Json.fromString(errs.toList.mkString(", ")))
              )
            } yield jpred)
            .leftMap(NonEmptyList.one)
            .map(jpred => (op: Option[Node]) => op.exists(_.evalXPath[Json](xp"self::node()").map(jpred).getOrElse(false)))
        case (XCdata, spec) =>
          Validated
            .fromEither(for {
              xspec <- spec
                .as[Map[Xpath, Map[Keyword.Xml, Json]]]
                .leftMap(_ => kwd.asInstanceOf[Keyword] -> spec)
              xpred <- XmlPredicate(xspec).toEither.leftMap(errs =>
                kwd.asInstanceOf[Keyword] -> Json.fromString(errs.toList.mkString(", "))
              )
            } yield xpred)
            .leftMap(NonEmptyList.one)
            .map(xpred =>
              (op: Option[Node]) =>
                op.exists(
                  _.evalXPath[String](xp"self::node()")
                    .flatMap(_.trim().asNode)
                    .map(xpred)
                    .getOrElse(false)
                )
            )
        case (kwd, j) => Validated.invalidNel(kwd -> j)
      }

  private def unsafeXPathExpr(xPathStr: String) = xPathFactory.newXPath().compile(xPathStr)
}
