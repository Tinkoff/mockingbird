import sbt.settingKey

object ProjectKeys {
  val dockerize = settingKey[Boolean]("Build native image inside of docker")
}
