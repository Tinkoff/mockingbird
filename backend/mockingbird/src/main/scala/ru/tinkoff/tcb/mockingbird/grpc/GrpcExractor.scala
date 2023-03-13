package ru.tinkoff.tcb.mockingbird.grpc

import scala.jdk.CollectionConverters.*

import com.github.os72.protobuf.dynamic.DynamicSchema
import com.github.os72.protobuf.dynamic.EnumDefinition
import com.github.os72.protobuf.dynamic.MessageDefinition
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DynamicMessage
import com.google.protobuf.util.JsonFormat
import io.circe.Json
import io.circe.parser.*
import io.estatico.newtype.ops.*
import mouse.boolean.*
import mouse.ignore
import org.apache.commons.io.output.ByteArrayOutputStream

import ru.tinkoff.tcb.mockingbird.model.*

object GrpcExractor {

  val primitiveTypes = Map(
    "TYPE_DOUBLE"   -> "double",
    "TYPE_FLOAT"    -> "float",
    "TYPE_INT32"    -> "int32",
    "TYPE_INT64"    -> "int64",
    "TYPE_UINT32"   -> "uint32",
    "TYPE_UINT64"   -> "uint64",
    "TYPE_SINT32"   -> "sint32",
    "TYPE_SINT64"   -> "sint64",
    "TYPE_FIXED32"  -> "fixed32",
    "TYPE_FIXED64"  -> "fixed64",
    "TYPE_SFIXED32" -> "sfixed32",
    "TYPE_SFIXED64" -> "sfixed64",
    "TYPE_BOOL"     -> "bool",
    "TYPE_STRING"   -> "string",
    "TYPE_BYTES"    -> "bytes"
  )

  def addSchemaToRegistry(schema: GrpcRootMessage, registry: DynamicSchema.Builder): Unit =
    schema match {
      case GrpcMessageSchema(name, fields, oneofs, nested) =>
        val builder = MessageDefinition
          .newBuilder(name)
        fields.foreach {
          case f if f.label == GrpcLabel.Optional =>
            val oneOfBuilder = builder.addOneof(s"_${f.name}")
            oneOfBuilder.addField(f.typeName, f.name, f.order)
          case f =>
            builder.addField(f.label.entryName, f.typeName, f.name, f.order)
        }
        oneofs.getOrElse(List.empty).foreach { oneof =>
          val oneOfBuilder = builder.addOneof(oneof.name)
          oneof.options.foreach { of =>
            oneOfBuilder.addField(of.typeName, of.name, of.order)
          }
        }
        nested.getOrElse(List.empty).foreach { nst =>
          val nestedBuilder = MessageDefinition.newBuilder(nst.name)
          nst.fields.foreach {
            case f if f.label == GrpcLabel.Optional =>
              val oneOfBuilder = nestedBuilder.addOneof(s"_${f.name}")
              oneOfBuilder.addField(f.typeName, f.name, f.order)
            case f =>
              nestedBuilder.addField(f.label.entryName, f.typeName, f.name, f.order)
          }
          builder.addMessageDefinition(nestedBuilder.build())
        }

        ignore(registry.addMessageDefinition(builder.build()))
      case GrpcEnumSchema(name, values) =>
        val builder = EnumDefinition
          .newBuilder(name)
        values.foreach { case (name, number) =>
          ignore(
            builder
              .addValue(
                name.asString,
                number.asInt
              )
          )
        }
        val enumDefinition = builder.build()
        ignore(registry.addEnumDefinition(enumDefinition))
    }

  implicit class FromGrpcProtoDefinition(private val definition: GrpcProtoDefinition) extends AnyVal {
    def toDynamicSchema: DynamicSchema = {
      val registryBuilder: DynamicSchema.Builder = DynamicSchema.newBuilder()
      val messageSchemas                         = definition.schemas
      registryBuilder.setName(definition.name)
      definition.`package`.foreach(registryBuilder.setPackage)
      messageSchemas.foreach(addSchemaToRegistry(_, registryBuilder))
      registryBuilder.build()
    }

    def parseFrom(bytes: Array[Byte], className: String): DynamicMessage =
      DynamicMessage.parseFrom(toDynamicSchema.getMessageDescriptor(className), bytes)

    def parseFromJson(response: Json, className: String): Array[Byte] = {
      val schema     = toDynamicSchema
      val msgBuilder = schema.newMessageBuilder(className)
      JsonFormat.parser().merge(response.spaces4, msgBuilder)
      val message      = msgBuilder.build()
      val outputStream = new ByteArrayOutputStream()
      message.writeTo(outputStream)
      outputStream.toByteArray
    }

    def convertMessageToJson(bytes: Array[Byte], className: String): Task[Json] =
      ZIO.fromEither {
        val message    = parseFrom(bytes, className)
        val jsonString = JsonFormat.printer().print(message)
        parse(jsonString)
      }
  }

  implicit class FromDynamicSchema(private val dynamicSchema: DynamicSchema) extends AnyVal {
    def toGrpcProtoDefinition: GrpcProtoDefinition = {
      val descriptor: DescriptorProtos.FileDescriptorProto = dynamicSchema.getFileDescriptorSet.getFile(0)
      val namespace                                        = descriptor.hasPackage.option(descriptor.getPackage)
      val enums = descriptor.getEnumTypeList.asScala.map { enum =>
        GrpcEnumSchema(
          enum.getName,
          enum.getValueList.asScala.map(i => (i.getName.coerce[FieldName], i.getNumber.coerce[FieldNumber])).toMap
        )
      }.toList
      val messages = descriptor.getMessageTypeList.asScala
        .filter(!_.getOptions.getMapEntry)
        .map(message2messageSchema)
        .toList
      GrpcProtoDefinition(
        descriptor.getName,
        enums ++ messages,
        namespace
      )
    }
  }

  private def message2messageSchema(message: DescriptorProtos.DescriptorProto): GrpcMessageSchema = {
    val (fields, oneofs) = message.getFieldList.asScala.toList
      .partition(f => !f.hasOneofIndex || isProto3OptionalField(f, message.getOneofDeclList.asScala.map(_.getName).toSet))

    val nested = message.getNestedTypeList.asScala.toList

    GrpcMessageSchema(
      message.getName,
      fields
        .map { field =>
          val label = GrpcLabel.withValue(field.getLabel.toString.split("_").last.toLowerCase).pipe { label =>
            if (
              label == GrpcLabel.Optional && (!isProto3OptionalField(
                field,
                message.getOneofDeclList.asScala.map(_.getName).toSet
              ))
            ) GrpcLabel.Required
            else label
          }
          getGrpcField(field, label)
        },
      oneofs
        .groupMap(_.getOneofIndex) { field =>
          getGrpcField(field, GrpcLabel.Optional)
        }
        .map { case (index, fields) =>
          GrpcOneOfSchema(
            message.getOneofDeclList.asScala(index).getName,
            fields
          )
        }
        .toList match {
        case Nil  => None
        case list => Some(list)
      },
      nested
        .map(message2messageSchema) match {
        case Nil  => None
        case list => Some(list)
      }
    )
  }

  private def isProto3OptionalField(field: DescriptorProtos.FieldDescriptorProto, oneOfFields: Set[String]): Boolean =
    GrpcLabel.withValue(field.getLabel.toString.split("_").last.toLowerCase) == GrpcLabel.Optional &&
      oneOfFields(s"_${field.getName}")

  private def getGrpcField(field: DescriptorProtos.FieldDescriptorProto, label: GrpcLabel): GrpcField = {
    val grpcType = getGrpcType(field)
    GrpcField(
      grpcType,
      label,
      getFieldType(field, grpcType == GrpcType.Custom),
      field.getName,
      field.getNumber
    )
  }

  private def getGrpcType(field: DescriptorProtos.FieldDescriptorProto): GrpcType =
    if (!primitiveTypes.isDefinedAt(field.getType.name()) || field.getTypeName != "") GrpcType.Custom
    else GrpcType.Primitive

  private def getFieldType(field: DescriptorProtos.FieldDescriptorProto, custom: Boolean): String =
    if (custom) field.getTypeName
    else primitiveTypes(field.getType.name())
}
