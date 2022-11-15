package ru.tinkoff.tcb.mockingbird.grpc

import java.io.ByteArrayInputStream
import java.io.InputStream

import io.grpc.MethodDescriptor
import io.grpc.MethodDescriptor.Marshaller

object Method {

  /*
  Универсальный маршаллер, который не меняет поток байтов
   */
  case class ByteMarshaller() extends Marshaller[Array[Byte]] {
    override def stream(value: Array[Byte]): InputStream = new ByteArrayInputStream(value)

    override def parse(stream: InputStream): Array[Byte] = stream.readAllBytes()
  }

  val byteMethod: MethodDescriptor[Array[Byte], Array[Byte]] =
    MethodDescriptor
      .newBuilder()
      .setType(MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(MethodDescriptor.generateFullMethodName("Any", "Any"))
      .setRequestMarshaller(ByteMarshaller())
      .setResponseMarshaller(ByteMarshaller())
      .build()

  def byteMethod(serviceName: String, methodName: String): MethodDescriptor[Array[Byte], Array[Byte]] =
    MethodDescriptor
      .newBuilder()
      .setType(MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, methodName))
      .setRequestMarshaller(ByteMarshaller())
      .setResponseMarshaller(ByteMarshaller())
      .build()

}
