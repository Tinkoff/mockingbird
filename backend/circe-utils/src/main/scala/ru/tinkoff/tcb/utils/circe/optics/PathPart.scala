package ru.tinkoff.tcb.utils.circe.optics

sealed private[optics] trait PathPart {
  def fold[T](onField: String => T, onIndex: Int => T, onTraverse: => T): T =
    this match {
      case Field(name)  => onField(name)
      case Index(index) => onIndex(index)
      case Traverse     => onTraverse
    }
}
final private[optics] case class Field(name: String) extends PathPart
final private[optics] case class Index(index: Int) extends PathPart
final private[optics] case object Traverse extends PathPart
