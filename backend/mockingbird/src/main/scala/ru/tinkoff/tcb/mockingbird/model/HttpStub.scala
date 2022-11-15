package ru.tinkoff.tcb.mockingbird.model

import java.time.Instant
import scala.util.matching.Regex

import com.github.dwickern.macros.NameOf.*
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import io.circe.Json
import mouse.boolean.*
import sttp.tapir.Schema.annotations.description
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID
import ru.tinkoff.tcb.utils.unpack.*
import ru.tinkoff.tcb.validation.Rule

@derive(bsonDecoder, bsonEncoder, encoder, decoder, schema)
final case class HttpStub(
    @BsonKey("_id")
    @description("id мока")
    id: SID[HttpStub],
    @description("Время создания мока")
    created: Instant,
    @description("Тип конфигурации")
    scope: Scope,
    @description("Количество возможных срабатываний. Имеет смысл только для scope=countdown")
    times: Option[Int] = Some(1),
    serviceSuffix: String,
    @description("Название мока")
    name: String,
    @description("HTTP метод")
    method: HttpMethod,
    @description("Суффикс пути, по которому срабатывает мок")
    path: Option[String],
    pathPattern: Option[Regex],
    seed: Option[Json],
    @description("Предикат для поиска состояния")
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    @description("Спецификация запроса")
    request: HttpStubRequest,
    @description("Данные, записываемые в базу")
    persist: Option[Map[JsonOptic, Json]],
    @description("Спецификация ответа")
    response: HttpStubResponse,
    @description("Спецификация колбека")
    callback: Option[Callback],
    @description("Тэги")
    labels: Seq[String] = Seq.empty
)

object HttpStub extends CallbackChecker {
  private val pathOrPattern: Rule[HttpStub] = stub =>
    (stub.path, stub.pathPattern) match {
      case Some(_) <*> None | None <*> Some(_) => Vector.empty
      case Some(_) <*> Some(_)                 => Vector("Может быть указан путь либо шаблон пути")
      case None <*> None                       => Vector("Должен быть указан путь либо шаблон пути")
    }

  private val stateNonEmpty: Rule[HttpStub] =
    _.state.exists(_.isEmpty).valueOrZero(Vector("Предикат state не может быть пустым"))

  private val persistNonEmpty: Rule[HttpStub] =
    _.persist.exists(_.isEmpty).valueOrZero(Vector("Спецификация persist не может быть пустой"))

  private val timesGreaterZero: Rule[HttpStub] =
    _.times.exists(_ <= 0).valueOrZero(Vector("times должно быть больше 0"))

  private val jsonProxyReq: Rule[HttpStub] = stub =>
    (stub.request, stub.response) match {
      case (JsonRequest(_, _, _) | JLensRequest(_, _, _), JsonProxyResponse(_, _, _, _)) => Vector.empty
      case (_, JsonProxyResponse(_, _, _, _)) =>
        Vector(s"${nameOfType[JsonProxyResponse]} может использоваться только совместно с ${nameOfType[JsonRequest]} или ${nameOfType[JLensRequest]}")
      case (XmlRequest(_, _, _, _, _) | XPathRequest(_, _, _, _, _), XmlProxyResponse(_, _, _, _)) => Vector.empty
      case (_, XmlProxyResponse(_, _, _, _)) =>
        Vector(s"${nameOfType[XmlProxyResponse]} может использоваться только совместно с ${nameOfType[XmlRequest]} или ${nameOfType[XPathRequest]}")
      case _ => Vector.empty
    }

  def validationRules(destinations: Set[SID[DestinationConfiguration]]): Rule[HttpStub] =
    Vector(
      pathOrPattern,
      (h: HttpStub) => checkCallback(h.callback, destinations),
      stateNonEmpty,
      persistNonEmpty,
      timesGreaterZero,
      jsonProxyReq
    ).reduce(_ |+| _)
}
