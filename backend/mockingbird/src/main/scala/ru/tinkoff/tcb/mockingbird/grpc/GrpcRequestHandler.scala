package ru.tinkoff.tcb.mockingbird.grpc

import scalapb.zio_grpc.RequestContext
import scalapb.zio_grpc.ZManagedChannel
import scalapb.zio_grpc.client.ClientCalls

import io.circe.Json
import io.circe.syntax.KeyOps
import io.grpc.CallOptions
import io.grpc.ManagedChannelBuilder
import zio.Duration

import ru.tinkoff.tcb.mockingbird.api.Tracing
import ru.tinkoff.tcb.mockingbird.api.WLD
import ru.tinkoff.tcb.mockingbird.error.StubSearchError
import ru.tinkoff.tcb.mockingbird.grpc.GrpcExractor.FromGrpcProtoDefinition
import ru.tinkoff.tcb.mockingbird.model.FillResponse
import ru.tinkoff.tcb.mockingbird.model.GProxyResponse
import ru.tinkoff.tcb.mockingbird.model.PersistentState
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.utils.transformation.json.*

trait GrpcRequestHandler {
  def exec(bytes: Array[Byte]): RIO[WLD & RequestContext, Array[Byte]]
}

class GrpcRequestHandlerImpl(stubResolver: GrpcStubResolver) extends GrpcRequestHandler {
  override def exec(bytes: Array[Byte]): RIO[WLD & RequestContext, Array[Byte]] =
    for {
      context <- ZIO.service[RequestContext]
      grpcServiceName = context.methodDescriptor.getFullMethodName
      f               = stubResolver.findStubAndState(grpcServiceName, bytes) _
      _ <- Tracing.update(_.addToPayload("service" -> grpcServiceName))
      (stub, req, stateOp) <- f(Scope.Countdown)
        .filterOrElse(_.isDefined)(f(Scope.Ephemeral).filterOrElse(_.isDefined)(f(Scope.Persistent)))
        .someOrFail(StubSearchError(s"Не удалось подобрать заглушку для $grpcServiceName"))
      _ <- Tracing.update(_.addToPayload("name" -> stub.name))
      seed = stub.seed.map(_.eval)
      state <- ZIO.fromOption(stateOp).orElse(PersistentState.fresh)
      data = Json.obj(
        "req" := req,
        "seed" := seed,
        "state" := state.data
      )
      responseSchema = stub.responseSchema
      response <- stub.response match {
        case FillResponse(rdata, delay) =>
          ZIO.when(delay.isDefined)(ZIO.sleep(Duration.fromScala(delay.get))) *>
            ZIO.attemptBlocking(responseSchema.parseFromJson(rdata.substitute(data), stub.responseClass))
        case GProxyResponse(endpoint, patch, delay) =>
          for {
            _          <- ZIO.when(delay.isDefined)(ZIO.sleep(Duration.fromScala(delay.get)))
            binaryResp <- proxyCall(endpoint, bytes)
            jsonResp   <- responseSchema.convertMessageToJson(binaryResp, stub.responseClass)
            patchedJsonResp   = jsonResp.patch(data, patch)
            patchedBinaryResp = responseSchema.parseFromJson(patchedJsonResp, stub.responseClass)
          } yield patchedBinaryResp
      }
    } yield response

  private def proxyCall(
      endpoint: String,
      bytes: Array[Byte]
  ): RIO[RequestContext, Array[Byte]] = {
    val mc: ZManagedChannel[Any] = ZManagedChannel(
      ManagedChannelBuilder.forTarget(endpoint).usePlaintext(),
    )

    ZIO.scoped {
      mc.flatMap { channel =>
        for {
          context <- ZIO.service[RequestContext]
          result <- ClientCalls
            .unaryCall(
              channel,
              Method.byteMethod(context.methodDescriptor.getServiceName, context.methodDescriptor.getFullMethodName),
              CallOptions.DEFAULT,
              context.metadata,
              bytes
            )
            .mapError(_.asRuntimeException())
        } yield result
      }
    }
  }
}

object GrpcRequestHandlerImpl {
  val live: URLayer[GrpcStubResolver, GrpcRequestHandler] = ZLayer.fromFunction(new GrpcRequestHandlerImpl(_))
}

object GrpcRequestHandler {
  def exec(bytes: Array[Byte]): RIO[WLD & RequestContext & GrpcRequestHandler, Array[Byte]] =
    for {
      service  <- ZIO.service[GrpcRequestHandler]
      response <- service.exec(bytes)
    } yield response
}
