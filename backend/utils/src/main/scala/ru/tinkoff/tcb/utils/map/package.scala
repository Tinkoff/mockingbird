package ru.tinkoff.tcb.utils

package object map {
  implicit class MapOps[K, V](map: Map[K, V]) {

    /**
     * Добавляет ключ если значение Some(..)
     */
    @inline def +?(kv: (K, Option[V])): Map[K, V] =
      kv._2.fold(map)(v => map + (kv._1 -> v))
  }
}
