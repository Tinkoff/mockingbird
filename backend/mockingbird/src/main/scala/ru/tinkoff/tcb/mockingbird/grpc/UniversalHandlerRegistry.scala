package ru.tinkoff.tcb.mockingbird.grpc

import io.grpc.HandlerRegistry
import io.grpc.ServerCallHandler
import io.grpc.ServerMethodDefinition

case class UniversalHandlerRegistry0(method: ServerMethodDefinition[?, ?]) extends HandlerRegistry {

  override def lookupMethod(methodName: String, authority: String): ServerMethodDefinition[?, ?] =
    method
}

case class UniversalHandlerRegistry(handler: ServerCallHandler[Array[Byte], Array[Byte]]) extends HandlerRegistry {

  override def lookupMethod(methodName: String, authority: String): ServerMethodDefinition[Array[Byte], Array[Byte]] = {
    val methodNameArray = methodName.split("/")
    val serviceName     = methodNameArray(0)
    val method          = methodNameArray(1)
    ServerMethodDefinition.create(Method.byteMethod(serviceName, method), handler)
  }
}
