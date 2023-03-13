package ru.tinkoff.tcb.protobuf

import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import scala.language.postfixOps
import scala.reflect.io.Directory
import scala.sys.process.*

import com.github.os72.protobuf.dynamic.DynamicSchema
import zio.managed.*
import zio.test.*

import ru.tinkoff.tcb.mockingbird.grpc.GrpcExractor.FromDynamicSchema
import ru.tinkoff.tcb.mockingbird.grpc.GrpcExractor.FromGrpcProtoDefinition

object MappersSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Mappers suite")(
      test("Mappers from DynamicSchema to GrpcProtoDefinition and back are consistent") {
        val managed = ZManaged.acquireReleaseWith(ZIO.attemptBlockingIO(Files.createTempDirectory("temp"))) { path =>
          ZIO.attemptBlockingIO {
            val dir = new Directory(path.toFile)
            dir.deleteRecursively()
          }.orDie
        }
        (for {
          path <- managed
          readStream <- ZManaged.fromAutoCloseable {
            ZIO.attemptBlockingIO(
              Files.newInputStream(
                Paths.get("./mockingbird/src/test/resources/requests.proto")
              )
            )
          }
          writeStream <- ZManaged.fromAutoCloseable {
            ZIO.attemptBlockingIO {
              Files.newOutputStream(Paths.get(s"${path.toString}/requests.proto"))
            }
          }
          _ <- ZIO.attemptBlockingIO(writeStream.write(readStream.readAllBytes())).toManaged
          _ <- ZManaged.succeed {
            s"protoc --descriptor_set_out=${path.toString}/descriptor.desc --proto_path=${path.toString} requests.proto" !
          }
          stream <- ZManaged.fromAutoCloseable {
            ZIO.attemptBlockingIO(new FileInputStream(new File(s"${path.toString}/descriptor.desc")))
          }
          content <- ZIO.attemptBlockingIO(stream.readAllBytes()).toManaged
          schema               = DynamicSchema.parseFrom(content)
          protoDefinition      = schema.toGrpcProtoDefinition
          protoDefinitionAgain = protoDefinition.toDynamicSchema.toGrpcProtoDefinition
        } yield assertTrue(protoDefinition == protoDefinitionAgain)).useNow
      },
      test("Mappers from nested DynamicSchema to GrpcProtoDefinition and back are consistent") {
        val managed = ZManaged.acquireReleaseWith(ZIO.attemptBlockingIO(Files.createTempDirectory("temp"))) { path =>
          ZIO.attemptBlockingIO {
            val dir = new Directory(path.toFile)
            dir.deleteRecursively()
          }.orDie
        }
        (for {
          path <- managed
          readStream <- ZManaged.fromAutoCloseable {
            ZIO.attemptBlockingIO(
              Files.newInputStream(
                Paths.get("./mockingbird/src/test/resources/nested.proto")
              )
            )
          }
          writeStream <- ZManaged.fromAutoCloseable {
            ZIO.attemptBlockingIO {
              Files.newOutputStream(Paths.get(s"${path.toString}/nested.proto"))
            }
          }
          _ <- ZIO.attemptBlockingIO(writeStream.write(readStream.readAllBytes())).toManaged
          _ <- ZManaged.succeed {
            s"protoc --descriptor_set_out=${path.toString}/nested_descriptor.desc --proto_path=${path.toString} nested.proto" !
          }
          stream <- ZManaged.fromAutoCloseable {
            ZIO.attemptBlockingIO(new FileInputStream(new File(s"${path.toString}/nested_descriptor.desc")))
          }
          content <- ZIO.attemptBlockingIO(stream.readAllBytes()).toManaged
          schema               = DynamicSchema.parseFrom(content)
          protoDefinition      = schema.toGrpcProtoDefinition
          protoDefinitionAgain = protoDefinition.toDynamicSchema.toGrpcProtoDefinition
        } yield assertTrue(protoDefinition == protoDefinitionAgain)).useNow
      }
    )
}
