package ru.tinkoff.tcb.mockingbird

import cron4s.Cron

package object stream {
  final val midnight = Cron.unsafeParse("0 0 0 ? * *")
}
