package ru.tinkoff.tcb.utils

import sttp.client3.RequestT

package object xttp {
  implicit class RequestTXtras[U[_], T, -R](private val rqt: RequestT[U, T, R]) extends AnyVal {
    def headersReplacing(hs: Map[String, String]): RequestT[U, T, R] =
      hs.foldLeft(rqt) { case (request, (key, value)) =>
        request.header(key, value, true)
      }
  }
}
