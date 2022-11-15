package ru.tinkoff.tcb.mockingbird.misc

import scala.xml.Node

import cats.tagless.finalAlg
import io.circe.Json
import kantan.xpath.Node as KNode

import ru.tinkoff.tcb.utils.transformation.json.*
import ru.tinkoff.tcb.utils.transformation.xml.*

/**
 * Свидетельство того, что B можно подставить в A
 */
@finalAlg trait Substitute[A, B] {
  def substitute(a: A, b: B): A
}

object Substitute {
  implicit val jsonSJson: Substitute[Json, Json]  = (a: Json, b: Json) => a.substitute(b)
  implicit val jsonSNode: Substitute[Json, KNode] = (a: Json, b: KNode) => a.substitute(b)
  implicit val nodeSJson: Substitute[Node, Json]  = (a: Node, b: Json) => a.substitute(b)
  implicit val nodeSnode: Substitute[Node, KNode] = (a: Node, b: KNode) => a.substitute(b)
}
