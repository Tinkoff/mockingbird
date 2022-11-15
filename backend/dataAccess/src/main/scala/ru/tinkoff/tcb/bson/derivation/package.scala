package ru.tinkoff.tcb.bson

import derevo.Derevo
import derevo.Derivation
import derevo.delegating

package object derivation {
  val ClassNameField = "className"

  @delegating("ru.tinkoff.tcb.bson.derivation.DerivedEncoder.genBsonEncoder")
  object bsonEncoder extends Derivation[BsonEncoder] {
    def instance[A]: BsonEncoder[A] = macro Derevo.delegate[BsonEncoder, A]
  }

  @delegating("ru.tinkoff.tcb.bson.derivation.DerivedDecoder.genBsonDecoder")
  object bsonDecoder extends Derivation[BsonDecoder] {
    def instance[A]: BsonDecoder[A] = macro Derevo.delegate[BsonDecoder, A]
  }
}
