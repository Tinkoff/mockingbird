package ru.tinkoff.tcb.bson.derivation

import magnolia1.*
import org.mongodb.scala.bson.*

import ru.tinkoff.tcb.bson.*
import ru.tinkoff.tcb.bson.annotation.BsonDiscriminator
import ru.tinkoff.tcb.bson.annotation.BsonKey

object DerivedEncoder {
  type Typeclass[T] = BsonEncoder[T]

  def join[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] =
    (value: T) =>
      BsonDocument(
        caseClass.parameters
          .map { p =>
            val fieldName =
              if (p.annotations.isEmpty) p.label
              else p.annotations.collectFirst { case BsonKey(value) => value }.getOrElse(p.label)

            fieldName -> p.typeclass.toBson(p.dereference(value))
          }
          .filterNot(_._2.isNull)
      )

  def split[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = (value: T) => {
    val (discriminatorField, renameFun) =
      if (sealedTrait.annotations.isEmpty) ClassNameField -> identity[String] _
      else
        sealedTrait.annotations
          .collectFirst { case BsonDiscriminator(d, rename) => d -> rename }
          .getOrElse(ClassNameField -> identity[String] _)

    sealedTrait.split(value) { st =>
      st.typeclass.toBson(st.cast(value)) match {
        case BDocument(fields) =>
          BsonDocument(fields + (discriminatorField -> BsonString(renameFun(st.typeName.short))))
        case other => other
      }
    }
  }

  def genBsonEncoder[T]: Typeclass[T] = macro Magnolia.gen[T]
}
