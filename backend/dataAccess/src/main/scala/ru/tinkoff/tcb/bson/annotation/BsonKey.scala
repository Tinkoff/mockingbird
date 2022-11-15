package ru.tinkoff.tcb.bson.annotation

import scala.annotation.StaticAnnotation

final case class BsonKey(value: String) extends StaticAnnotation
