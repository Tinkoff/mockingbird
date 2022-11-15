package ru.tinkoff.tcb.bson.derivation

import java.time.Instant
import java.time.Year

import derevo.derive

import ru.tinkoff.tcb.bson.annotation.BsonKey

@derive(bsonDecoder, bsonEncoder)
case class TestMeta(time: Instant, seq: Long, flag: Boolean)

@derive(bsonDecoder, bsonEncoder)
case class TestCheck(year: Year, comment: String)

@derive(bsonDecoder, bsonEncoder)
case class TestEntity(
    @BsonKey("_id") id: Int,
    name: String,
    meta: TestMeta,
    comment: Option[String],
    linkId: Option[Int],
    checks: Seq[TestCheck]
)

@derive(bsonDecoder, bsonEncoder)
case class TestContainer[T](value: Option[T])

@derive(bsonDecoder, bsonEncoder)
case class TestEntityWithDefaults(
    @BsonKey("_id") id: Int,
    name: String = "test",
    meta: TestMeta,
    comment: Option[String],
    linkId: Option[Int],
    checks: Seq[TestCheck] = Seq()
)
