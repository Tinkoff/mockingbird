package ru.tinkoff.tcb.mockingbird.model

import com.github.dwickern.macros.NameOf.nameOf
import mouse.all.booleanSyntaxMouse

import ru.tinkoff.tcb.utils.id.SID
import ru.tinkoff.tcb.utils.unpack.*

trait CallbackChecker {

  protected def checkCallback(
      callback: Option[Callback],
      destinations: Set[SID[DestinationConfiguration]]
  ): Vector[String] =
    callback match {
      case None => Vector.empty
      case Some(value) =>
        value match {
          case MessageCallback(dest, _, mcallback, _) =>
            (destinations(dest) !? Vector(s"$dest не настроен")) ++ checkCallback(mcallback, destinations)
          case HttpCallback(_, rm, p, hcallback, _) =>
            (rm, p) match {
              case Some(_) <*> None =>
                s"Поле ${nameOf[HttpCallback](_.responseMode)} должно быть заполнено ТОЛЬКО при наличии ${nameOf[HttpCallback](_.persist)}" +: checkCallback(
                  hcallback,
                  destinations
                )
              case None <*> Some(_) =>
                s"Поле ${nameOf[HttpCallback](_.responseMode)} должно быть заполнено при наличии ${nameOf[HttpCallback](_.persist)}" +: checkCallback(
                  hcallback,
                  destinations
                )
              case _ => checkCallback(hcallback, destinations)
            }
        }
    }

}
