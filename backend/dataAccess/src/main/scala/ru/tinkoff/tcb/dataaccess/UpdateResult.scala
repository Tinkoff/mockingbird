package ru.tinkoff.tcb.dataaccess

final case class UpdateResult(matched: Long, modified: Long) {
  val successful: Boolean   = matched > 0 || modified > 0
  val noMatch: Boolean      = matched == 0
  val noOp: Boolean         = modified == 0
  val unsuccessful: Boolean = noMatch && noOp
}

object UpdateResult {
  val empty: UpdateResult = UpdateResult(0, 0)

  def apply(affected: Long): UpdateResult = UpdateResult(affected, affected)
}
