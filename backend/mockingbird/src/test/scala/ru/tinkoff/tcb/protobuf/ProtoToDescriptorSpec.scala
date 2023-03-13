package ru.tinkoff.tcb.protobuf

import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import scala.jdk.CollectionConverters.SetHasAsScala
import scala.language.postfixOps
import scala.reflect.io.Directory
import scala.sys.process.*

import com.github.os72.protobuf.dynamic.DynamicSchema
import zio.managed.*
import zio.test.*

object ProtoToDescriptorSpec extends ZIOSpecDefault {

  val messageTypes: Set[String] = Set("CarGenRequest", "CarSearchRequest", "MemoRequest")

  val nestedMessageTypes: Set[String] = Set(
    "utp.stock_service.v1.GetStocksRequest",
    "utp.stock_service.v1.GetStocksResponse",
    "utp.stock_service.v1.GetStocksResponse.Stock",
    "utp.stock_service.v1.GetStocksResponse.Stocks"
  )

  override def spec: Spec[TestEnvironment, Any] =
    suite("Proto to descriptor")(
      test("DynamicSchema is successfully parsed from proto file") {
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
          schema = DynamicSchema.parseFrom(content)
        } yield assertTrue(messageTypes.subsetOf(schema.getMessageTypes.asScala))).useNow
      },
      test("DynamicSchema is successfully parsed from proto file with nested schema") {
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
          schema = DynamicSchema.parseFrom(content)
        } yield assertTrue(nestedMessageTypes.subsetOf(schema.getMessageTypes.asScala))).useNow
      }
    )
}
