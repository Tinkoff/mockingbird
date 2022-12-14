package ru.tinkoff.tcb.bson.enumeratum.values

import enumeratum.values.*

sealed abstract class BsonContentType(val value: Long, name: String) extends LongEnumEntry

case object BsonContentType extends LongEnum[BsonContentType] with LongBsonValueEnum[BsonContentType] {

  val values = findValues

  case object Text extends BsonContentType(value = 1L, name = "text")
  case object Image extends BsonContentType(value = 2L, name = "image")
  case object Video extends BsonContentType(value = 3L, name = "video")
  case object Audio extends BsonContentType(value = 4L, name = "audio")

}

sealed abstract class BsonLibraryItem(val value: Int, val name: String) extends IntEnumEntry

case object BsonLibraryItem extends IntEnum[BsonLibraryItem] with IntBsonValueEnum[BsonLibraryItem] {

  // A good mix of named, unnamed, named + unordered args
  case object Book extends BsonLibraryItem(value = 1, name = "book")
  case object Movie extends BsonLibraryItem(name = "movie", value = 2)
  case object Magazine extends BsonLibraryItem(3, "magazine")
  case object CD extends BsonLibraryItem(4, name = "cd")

  val values = findValues

}

sealed abstract class BsonOperatingSystem(val value: String) extends StringEnumEntry

case object BsonOperatingSystem extends StringEnum[BsonOperatingSystem] with StringBsonValueEnum[BsonOperatingSystem] {

  case object Linux extends BsonOperatingSystem("linux")
  case object OSX extends BsonOperatingSystem("osx")
  case object Windows extends BsonOperatingSystem("windows")
  case object Android extends BsonOperatingSystem("android")

  val values = findValues

}
