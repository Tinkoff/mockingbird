package ru.tinkoff.tcb.bson.derivation

import scala.util.Failure
import scala.util.Success

import magnolia1.*
import org.mongodb.scala.bson.*

import ru.tinkoff.tcb.bson.*
import ru.tinkoff.tcb.bson.annotation.BsonDiscriminator
import ru.tinkoff.tcb.bson.annotation.BsonKey

object DerivedDecoder {
  type Typeclass[T] = BsonDecoder[T]

  def join[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] =
    BsonDecoder.ofDocument { doc =>
      caseClass.constructMonadic { f =>
        val fieldName =
          if (f.annotations.isEmpty) f.label
          else f.annotations.collectFirst { case BsonKey(value) => value }.getOrElse(f.label)

        f.typeclass.fromBson(doc.getFieldOpt(fieldName).getOrElse(BsonNull())) match {
          case Failure(_) if f.default.isDefined => Success(f.default.get)
          case Failure(exc) =>
            Failure(DeserializationError(s"Unable to decode field ${f.label}", exc))
          case otherwise => otherwise
        }
      }
    }

  def split[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] =
    BsonDecoder.ofDocument { doc =>
      val (discriminatorField, renameFun) =
        if (sealedTrait.annotations.isEmpty) ClassNameField -> identity[String] _
        else
          sealedTrait.annotations
            .collectFirst { case BsonDiscriminator(d, rename) => d -> rename }
            .getOrElse(ClassNameField -> identity[String] _)

      for {
        discriminator <- doc
          .getFieldOpt(discriminatorField)
          .toRight(
            DeserializationError(
              s"No discriminator field ($discriminatorField) found while decoding ${sealedTrait.typeName.short}"
            )
          )
          .toTry
        typeName <- discriminator.decodeAs[String]
        decoder <- sealedTrait.subtypes
          .find(st => renameFun(st.typeName.short) == typeName)
          .toRight(DeserializationError(s"No case $typeName in ${sealedTrait.typeName.short}"))
          .toTry
        instance = decoder.typeclass
        result <- instance.fromBson(doc)
      } yield result
    }

  def genBsonDecoder[T]: Typeclass[T] = macro Magnolia.gen[T]
}
