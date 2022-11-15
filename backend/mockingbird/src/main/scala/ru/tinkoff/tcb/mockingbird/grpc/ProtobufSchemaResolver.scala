package ru.tinkoff.tcb.mockingbird.grpc

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import scala.language.postfixOps
import scala.reflect.io.Directory
import scala.sys.process.*

import com.github.os72.protobuf.dynamic.DynamicSchema
import zio.managed.*

import ru.tinkoff.tcb.logging.MDCLogging
import ru.tinkoff.tcb.mockingbird.api.Tracing
import ru.tinkoff.tcb.mockingbird.grpc.GrpcExractor.FromDynamicSchema
import ru.tinkoff.tcb.mockingbird.model.GrpcProtoDefinition

trait ProtobufSchemaResolver {
  def parseDefinitionFrom(bytes: Array[Byte]): ZIO[Tracing, IOException, GrpcProtoDefinition]
}

class ProtobufSchemaResolverImpl extends ProtobufSchemaResolver {

  private val log = MDCLogging.`for`[Tracing](this)

  def parseDefinitionFrom(bytes: Array[Byte]): ZIO[Tracing, IOException, GrpcProtoDefinition] = {
    val managed = ZManaged.acquireReleaseWith(ZIO.attemptBlockingIO(Files.createTempDirectory("temp"))) { path =>
      log.info("Deleting files in path", path.toString) *>
        ZIO.attemptBlockingIO {
          val dir = new Directory(path.toFile)
          dir.deleteRecursively()
        }.orDie
    }
    for {
      path <- managed
      writeStream <- ZManaged.fromAutoCloseable {
        ZIO.attemptBlockingIO {
          Files.newOutputStream(Paths.get(s"${path.toString}/requests.proto"))
        }
      }
      _ <- ZIO.attemptBlockingIO(writeStream.write(bytes)).toManaged
      _ <- ZManaged.succeed {
        s"protoc --descriptor_set_out=${path.toString}/descriptor.desc --proto_path=${path.toString} requests.proto" !
      }
      stream <- ZManaged.fromAutoCloseable {
        ZIO.attemptBlockingIO(new FileInputStream(new File(s"${path.toString}/descriptor.desc")))
      }
      content <- ZIO.attemptBlockingIO(stream.readAllBytes()).toManaged
      schema = DynamicSchema.parseFrom(content)
    } yield schema.toGrpcProtoDefinition
  }.useNow
}

object ProtobufSchemaResolverImpl {
  val live: ULayer[ProtobufSchemaResolver] = ZLayer.succeed(new ProtobufSchemaResolverImpl)
}
