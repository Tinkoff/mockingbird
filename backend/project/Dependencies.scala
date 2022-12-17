import sbt._

object Dependencies {
  val tapirBase = Seq("tapir-core", "tapir-enumeratum", "tapir-derevo", "tapir-refined")
    .map("com.softwaremill.sttp.tapir" %% _ % Versions.tapir)

  val tapir = Seq(
    "tapir-vertx-server-zio",
    "tapir-json-circe",
    "tapir-swagger-ui-bundle"
  ).map("com.softwaremill.sttp.tapir" %% _ % Versions.tapir)

  val tofu = Seq(
    "tofu-logging",
    "tofu-logging-structured",
    "tofu-logging-derivation"
  ).map("tf.tofu" %% _ % "0.11.1")

  val alleycats = Seq(
    "alleycats-core"
  ).map("org.typelevel" %% _ % Versions.cats)

  val cats = Seq(
    "cats-core",
    "cats-kernel"
  ).map("org.typelevel" %% _ % Versions.cats)

  val catsTagless = Seq("org.typelevel" %% "cats-tagless-macros" % "0.12")

  val zio = Seq(
    "dev.zio" %% "zio"                 % "2.0.0",
    "dev.zio" %% "zio-managed"         % "2.0.0",
    "dev.zio" %% "zio-interop-cats"    % "22.0.0.0",
    "dev.zio" %% "zio-interop-twitter" % "21.2.0.2.1",
    "dev.zio" %% "zio-test"            % "2.0.0" % Test,
    "dev.zio" %% "zio-test-sbt"        % "2.0.0" % Test
  )

  val json = Seq(
    "io.circe" %% "circe-core"                   % "0.14.1",
    "io.circe" %% "circe-generic"                % "0.14.1",
    "io.circe" %% "circe-parser"                 % "0.14.1",
    "io.circe" %% "circe-generic-extras"         % "0.14.1",
    "io.circe" %% "circe-literal"                % "0.14.1",
    "io.circe" %% "circe-jawn"                   % "0.14.1",
    "io.circe" %% "circe-derivation"             % "0.13.0-M5",
    "io.circe" %% "circe-derivation-annotations" % "0.13.0-M5",
    "io.circe" %% "circe-refined"                % "0.14.1"
  )

  def reflect(scalaVersion: String) = Seq("org.scala-lang" % "scala-reflect" % scalaVersion)

  val mouse = Seq("org.typelevel" %% "mouse" % "1.0.11")

  val enumeratum = Seq(
    "com.beachape" %% "enumeratum"       % "1.7.0",
    "com.beachape" %% "enumeratum-circe" % "1.7.0"
  )

  val scalatest = Seq(
    "org.scalatest"    %% "scalatest"      % "3.2.2" % Test,
    "com.ironcorelabs" %% "cats-scalatest" % "3.1.1" % Test
  )

  val scalacheck = Seq(
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.2.0" % Test,
    "org.scalacheck"    %% "scalacheck"      % "1.15.2"  % Test
  )

  lazy val refined = Seq(
    "eu.timepit" %% "refined" % "0.9.28"
  )

  lazy val protobuf = Seq(
    "io.grpc"               % "grpc-netty"           % "1.43.2",
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
    "com.google.protobuf"   % "protobuf-java"        % "3.19.3",
    "com.google.protobuf"   % "protobuf-java-util"   % "3.19.3"
  )

  lazy val metrics: Seq[ModuleID] = Seq(
    "io.micrometer"       % "micrometer-core"                % Versions.micrometer,
    "io.micrometer"       % "micrometer-registry-prometheus" % Versions.micrometer,
    "io.github.mweirauch" % "micrometer-jvm-extras"          % "0.2.2"
  )

  lazy val glass = Seq(
    "glass-core",
    "glass-macro",
  ).map("tf.tofu" %% _ % Versions.glass)
}
