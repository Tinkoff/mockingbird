package ru.tinkoff.tcb.utils

import scala.io.Source
import scala.util.Using

package object resource {
  def getResPath(fileName: String): String = getClass.getResource(s"/$fileName").getPath

  def readBytes(fileName: String): Array[Byte] = {
    val path = getResPath(fileName)
    import java.nio.file.{Files, Paths}
    Files.readAllBytes(Paths.get(path))
  }

  def readStr(fileName: String): String = Using.resource(Source.fromFile(getResPath(fileName)))(_.mkString)
}
