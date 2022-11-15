package ru.tinkoff.tcb.bson

import scala.jdk.CollectionConverters.*

import org.mongodb.scala.bson.*

import ru.tinkoff.tcb.bson.BsonEncoder.ops.*
import ru.tinkoff.tcb.generic.RootOptionFields

object PatchGenerator {
  def mkPatch[T: BsonEncoder](
      entity: T
  )(implicit rof: RootOptionFields[T]): (Option[BsonValue], BsonDocument) = {
    val bsonEntity = entity.bson.asDocument()

    val fieldToUnset  = rof.fields -- bsonEntity.keySet().asScala
    val unsetElements = fieldToUnset.toVector.map(_ -> BsonString(""))
    val unset =
      if (unsetElements.nonEmpty) Seq("$unset" -> BsonDocument(unsetElements)) else Seq.empty

    bsonEntity.getFieldOpt("_id") -> BsonDocument("$set" -> bsonEntity.tap(_.remove("_id"))).tap { doc =>
      unset.foreach { case (key, value) =>
        doc.append(key, value)
      }
    }
  }
}
