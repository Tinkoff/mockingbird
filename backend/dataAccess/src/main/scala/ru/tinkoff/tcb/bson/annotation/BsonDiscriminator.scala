package ru.tinkoff.tcb.bson.annotation

import scala.annotation.StaticAnnotation

final case class BsonDiscriminator(name: String, renameValues: String => String = identity) extends StaticAnnotation
