package ru.tinkoff.tcb

import ru.tinkoff.tcb.mockingbird.api.Tracing
import ru.tinkoff.tcb.mockingbird.api.WLD
import ru.tinkoff.tcb.mockingbird.config.SecurityConfig
import ru.tinkoff.tcb.utils.crypto.AES
import ru.tinkoff.tcb.utils.crypto.SyncAES

package object mockingbird {
  val wldRuntime: Runtime[WLD] =
    Unsafe.unsafe { implicit uns =>
      Runtime.unsafe.fromLayer(Tracing.live)
    }

  val aesEncoder: URLayer[SecurityConfig, AES] =
    ZLayer.fromFunction((sc: SecurityConfig) => new SyncAES(sc.secret))
}
