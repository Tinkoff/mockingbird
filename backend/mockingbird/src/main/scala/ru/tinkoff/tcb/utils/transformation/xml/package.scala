package ru.tinkoff.tcb.utils.transformation

import java.util.UUID
import scala.util.Random
import scala.util.Try
import scala.util.control.TailCalls
import scala.util.control.TailCalls.TailRec
import scala.xml.Attribute
import scala.xml.Elem
import scala.xml.Node
import scala.xml.Null
import scala.xml.PCData
import scala.xml.Text

import advxml.implicits.*
import advxml.transform.XmlModifier.*
import advxml.transform.XmlZoom
import io.circe.Json
import kantan.xpath.Node as KNode
import kantan.xpath.implicits.*

import ru.tinkoff.tcb.utils.json.json2StringFolder
import ru.tinkoff.tcb.utils.regex.OneOrMore
import ru.tinkoff.tcb.utils.transformation.json.jsonTemplater
import ru.tinkoff.tcb.xpath.*

package object xml {
  private object SubstRxs extends OneOrMore(SubstRx)

  private def nt(values: KNode): PartialFunction[String, Option[String]] = { case SubstRx(Xexpr(xe)) =>
    values.evalXPath[String](xe).toOption
  }

  private def nt(values: Node): PartialFunction[String, Option[String]] = { case SubstRx(XZoom(zoom)) =>
    zoom.bind(values).run[Option].map(_.text)
  }

  def nodeTemplater(values: KNode): PartialFunction[String, String] =
    nt(values).andThen { case Some(s) => s }

  def nodeTemplater(values: Node): PartialFunction[String, String] =
    nt(values).andThen { case Some(s) => s }

  implicit class XmlTransformation(private val n: Node) extends AnyVal {
    def isTemplate: Boolean =
      n match {
        case elem: Elem =>
          elem.attributes.exists(attr =>
            attr.value match {
              case Seq(Text(SubstRx(Xexpr(_)))) => true
              case Seq(Text(FunRx()))           => true
              case Seq(Text(SubstRxs()))        => true
              case _                            => false
            }
          ) || elem.child.exists(_.isTemplate)
        case Text(SubstRx(Xexpr(_))) => true
        case Text(FunRx())           => true
        case Text(SubstRxs())        => true
        case _                       => false
      }

    def transform(pf: PartialFunction[Node, Node]): TailRec[Node] =
      pf.applyOrElse(n, identity[Node]) match {
        case elem: Elem => elem.child.toVector.traverse(_.transform(pf)).map(children => elem.copy(child = children))
        case otherwise  => TailCalls.done(otherwise)
      }

    def substitute(values: KNode): Node =
      nodeTemplater(values).pipe { templater =>
        transform {
          case elem: Elem =>
            elem.attributes.foldLeft(elem)((e, attr) =>
              attr.value match {
                case Seq(Text(text)) =>
                  templater
                    .andThen(s => e % Attribute(None, attr.key, Text(s), Null))
                    .applyOrElse(text, (_: String) => e)
                case _ => e
              }
            )
          case Text(str) =>
            templater.andThen(Text(_)).applyOrElse(str, Text(_))
        }.result
      }

    def substitute(values: Node): Node =
      nodeTemplater(values).pipe { templater =>
        transform {
          case elem: Elem =>
            elem.attributes.foldLeft(elem)((e, attr) =>
              attr.value match {
                case Seq(Text(text)) =>
                  templater
                    .andThen(s => e % Attribute(None, attr.key, Text(s), Null))
                    .applyOrElse(text, (_: String) => e)
                case _ => e
              }
            )
          case Text(str) =>
            templater.andThen(Text(_)).applyOrElse(str, Text(_))
        }.result
      }

    def substitute(values: Json): Node =
      jsonTemplater(values).pipe { templater =>
        transform {
          case elem: Elem =>
            elem.attributes.foldLeft(elem)((e, attr) =>
              attr.value match {
                case Seq(Text(text)) =>
                  templater
                    .andThen(s => e % Attribute(None, attr.key, Text(s.foldWith(json2StringFolder)), Null))
                    .applyOrElse(text, (_: String) => e)
                case _ => e
              }
            )
          case Text(str) =>
            templater.andThen(_.foldWith(json2StringFolder)).andThen(Text(_)).applyOrElse(str, Text(_))
        }.result
      }

    def eval: Node =
      transform {
        case Text(RandStr(len)) =>
          Text(Random.alphanumeric.take(len.toInt).mkString)
        case Text(RandAlphabetStr(alphabet, minLen, maxLen)) =>
          Text(
            List.fill(Random.between(minLen.toInt, maxLen.toInt))(alphabet(Random.nextInt(alphabet.length))).mkString
          )
        case Text(RandInt(max)) =>
          Text(Random.nextInt(max.toInt).toString)
        case Text(RandIntInterval(min, max)) =>
          Text(Random.between(min.toInt, max.toInt).toString)
        case Text(RandLong(max)) =>
          Text(Random.nextLong(max.toLong).toString)
        case Text(RandLongInterval(min, max)) =>
          Text(Random.between(min.toLong, max.toLong).toString)
        case Text(RandUUID()) =>
          Text(UUID.randomUUID().toString)
      }.result

    def inlineXmlFromCData: Node =
      transform { case PCData(XCData(xml)) =>
        xml
      }.result

    def patchFromValues(jValues: Json, xValues: Node, schema: Map[XmlZoom, String]): Node = {
      val jt = jsonTemplater(jValues)
      val nt = nodeTemplater(<wrapper>{xValues}</wrapper>)

      schema
        .foldLeft(<wrapper>{n}</wrapper>: Node) { case (acc, (zoom, defn)) =>
          defn match {
            case jp if jt.isDefinedAt(jp) =>
              (zoom ==> Replace(_.map(_.transform { case Text(_) => Text(jt(jp).foldWith(json2StringFolder)) }.result)))
                .transform[Try](acc)
                .map(_.head)
                .getOrElse(acc)
            case xp if nt.isDefinedAt(xp) =>
              (zoom ==> Replace(_.map(_.transform { case Text(_) => Text(nt(xp)) }.result)))
                .transform[Try](acc)
                .map(_.head)
                .getOrElse(acc)
            case _ => acc
          }
        }
        .pipe(_.child.head)
    }
  }
}
