package ru.tinkoff.tcb.bson.enumeratum.values

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class EnumBsonHandlerSpec extends AnyFunSpec with Matchers with EnumBsonHandlerHelpers {
  describe(".reader") {

    testReader("IntEnum", BsonLibraryItem)
    testReader("LongEnum", BsonContentType)
    testReader("StringEnum", BsonOperatingSystem)

  }
}
