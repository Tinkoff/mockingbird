import ProjectKeys._
import ch.epfl.scala.sbtmissinglink.MissingLinkPlugin.missinglinkConflictsTag
import sbt.Keys.concurrentRestrictions

ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value)

ThisBuild / concurrentRestrictions += Tags.limit(missinglinkConflictsTag, 1)

ThisBuild / evictionErrorLevel := Level.Debug

val utils = (project in file("utils"))
  .settings(Settings.common)
  .settings(
    libraryDependencies ++= Dependencies.zio ++ Dependencies.scalatest ++ Dependencies.metrics ++ Dependencies.tofu
  )

val circeUtils = (project in file("circe-utils"))
  .settings(Settings.common)
  .settings(
    libraryDependencies ++= Dependencies.json ++ Dependencies.zio ++ Dependencies.scalatest
  )

val examples = (project in file("examples"))
  .enablePlugins(
    JavaAppPackaging
  )
  .dependsOn(utils, circeUtils)
  .settings(Settings.common)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.cats,
      Dependencies.tofu,
      Dependencies.mouse,
      Dependencies.enumeratum,
      Dependencies.scalatestMain,
      Dependencies.scalamock,
      Dependencies.refined,
    ).flatten,
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "armeria-backend-zio" % Versions.sttp,
      "com.softwaremill.sttp.client3" %% "circe"               % Versions.sttp,
      "pl.muninn"                     %% "scala-md-tag"        % "0.2.3",
      "dev.zio"                       %% "zio-cli"             % "0.5.0",
    ),
  )
  .settings(
    Compile / doc / sources := (file("examples/src") ** "*.scala").get,
    Compile / doc / scalacOptions ++= Seq("-groups", "-skip-packages", "sttp")
  )
  .settings(
    addCommandAlias(
      "fixCheck",
      "scalafixAll --check; scalafmtCheck"
    ),
    addCommandAlias(
      "lintAll",
      "scalafixAll; scalafmtAll"
    ),
    addCommandAlias(
      "simulacrum",
      "scalafixEnable;scalafix AddSerializable;scalafix AddImplicitNotFound;scalafix TypeClassSupport;"
    )
  )

val dataAccess = (project in file("dataAccess"))
  .settings(Settings.common)
  .settings(
    scalacOptions += "-language:experimental.macros",
    libraryDependencies ++= Dependencies.alleycats ++ Dependencies.cats ++ Dependencies.zio ++ Dependencies.catsTagless ++ Dependencies.mouse ++ Seq(
      "com.beachape"                 %% "enumeratum"                      % "1.7.0",
      "org.mongodb.scala"            %% "mongo-scala-driver"              % Versions.mongoScalaDriver,
      "org.typelevel"                %% "simulacrum-scalafix-annotations" % Versions.simulacrum,
      "com.chuusai"                  %% "shapeless"                       % "2.3.3",
      "org.julienrf"                 %% "enum-labels"                     % "3.1",
      "tf.tofu"                      %% "derevo-core"                     % Versions.derevo,
      "com.softwaremill.magnolia1_2" %% "magnolia"                        % "1.1.2",
      "com.google.code.findbugs"      % "jsr305"                          % "3.0.2" % Optional
    ) ++ Dependencies.scalatest ++ Dependencies.scalacheck ++ Dependencies.json ++ Dependencies.refined,
    libraryDependencies ++= Dependencies.reflect(scalaVersion.value),
    publish := {}
  )

val mockingbird = (project in file("mockingbird"))
  .aggregate(utils, circeUtils, dataAccess)
  .dependsOn(utils, circeUtils, dataAccess)
  .settings(Settings.common)
  .settings(
    name := "mockingbird",
    libraryDependencies ++= Seq(
      Dependencies.cats,
      Dependencies.catsTagless,
      Dependencies.enumeratum,
      Dependencies.scalatest,
      Dependencies.tofu,
      Dependencies.zio,
      Dependencies.refined,
      Dependencies.protobuf,
      Dependencies.tapirBase,
      Dependencies.glass
    ).flatten,
    libraryDependencies ++= Seq(
      "com.iheart"                    %% "ficus"               % "1.5.0",
      "io.circe"                      %% "circe-config"        % "0.8.0",
      "com.nrinaudo"                  %% "kantan.xpath"        % "0.5.2",
      "com.lihaoyi"                   %% "scalatags"           % "0.9.1",
      "tf.tofu"                       %% "derevo-circe"        % Versions.derevo,
      "org.webjars.npm"                % "swagger-ui-dist"     % "3.32.5",
      "eu.timepit"                    %% "fs2-cron-core"       % "0.2.2",
      "com.softwaremill.sttp.client3" %% "armeria-backend-zio" % Versions.sttp,
      "com.softwaremill.sttp.client3" %% "circe"               % Versions.sttp,
      "org.javassist"                  % "javassist"           % "3.29.2-GA", // Armeria dependency
      "org.apache.tika"                % "tika-core"           % "2.1.0",
      "io.scalaland"                  %% "chimney"             % "0.6.1",
      "com.ironcorelabs"              %% "cats-scalatest"      % "3.0.8" % Test,
      "com.google.code.findbugs"       % "jsr305"              % "3.0.2" % Optional,
      "com.github.dwickern"           %% "scala-nameof"        % "4.0.0" % Provided,
      "com.github.os72"                % "protobuf-dynamic"    % "1.0.1",
      "com.github.geirolz"            %% "advxml-core"         % "2.5.1",
      "com.github.geirolz"            %% "advxml-xpath"        % "2.5.1",
      "io.estatico"                   %% "newtype"             % "0.4.4",
      "org.slf4j"                      % "slf4j-api"           % "1.7.30" % Provided
    ),
    Compile / unmanagedResourceDirectories += file("../frontend/dist")
  )
  .settings(
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = true)          -> (Compile / sourceManaged).value / "scalapb",
      scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value / "scalapb"
    )
  )

/*
   Отдельный подпроект создан ради того, чтобы не отключать coursier во всём проекте.
   Предполагается, что после того, как починят https://github.com/coursier/coursier/issues/2016
   можно будет перенести код отсюда в mockingbird
 */
lazy val `mockingbird-api` = (project in file("mockingbird-api"))
  .enablePlugins(BuildInfoPlugin)
  .aggregate(mockingbird)
  .dependsOn(mockingbird)
  .settings(Settings.common)
  .configure(Settings.docker("ru.tinkoff.tcb.mockingbird.Mockingbird", "mockingbird", 8228 :: 9000 :: Nil))
  .settings(
    name := "mockingbird-api",
    libraryDependencies ++= Seq(
      Dependencies.tapir
    ).flatten,
    useCoursier := false, // https://github.com/coursier/coursier/issues/2016
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "ru.tinkoff.tcb.mockingbird.build",
    Compile / packageDoc / mappings := Seq(),
    run / fork := true,
    run / javaOptions += "-Dconfig.resource=local.conf",
    Compile / unmanagedResourceDirectories += file("../frontend/dist")
  )
  .settings(
    addCommandAlias(
      "fixCheck",
      "scalafixAll --check; scalafmtCheck"
    ),
    addCommandAlias(
      "lintAll",
      "scalafixAll; scalafmtAll"
    ),
    addCommandAlias(
      "simulacrum",
      "scalafixEnable;scalafix AddSerializable;scalafix AddImplicitNotFound;scalafix TypeClassSupport;"
    )
  )

lazy val `mockingbird-native` = (project in file("mockingbird-native"))
  .dependsOn(`mockingbird-api`)
  .enablePlugins(
    GraalVMNativeImagePlugin,
    NativeImagePlugin
  )
  .settings(libraryDependencies -= "org.scalameta" % "svm-subs" % "101.0.0")
  .aggregate(mockingbird)
  .dependsOn(mockingbird)
  .settings(Settings.common)
  .configure(Settings.dockerNative("mockingbird-native", 8228 :: 9000 :: Nil))
  .settings(
    name := "mockingbird-native",
    useCoursier := false, // https://github.com/coursier/coursier/issues/2016
    Compile / run / mainClass := Some("ru.tinkoff.tcb.mockingbird.Mockingbird"),
    Compile / packageDoc / mappings := Seq(),
    GraalVMNativeImage / mainClass := Some("ru.tinkoff.tcb.mockingbird.Mockingbird"),
    GraalVMNativeImage / graalVMNativeImageOptions ++= Seq("-H:+StaticExecutableWithDynamicLibC").filter(_ =>
      dockerize.value
    ),
    nativeImageInstalled := true,
    nativeImageAgentMerge := true,
    run / javaOptions += "-Dconfig.resource=local.conf",
  )
  .settings(
    addCommandAlias(
      "fixCheck",
      "scalafixAll --check; scalafmtCheck"
    ),
    addCommandAlias(
      "lintAll",
      "scalafixAll; scalafmtAll"
    ),
    addCommandAlias(
      "simulacrum",
      "scalafixEnable;scalafix AddSerializable;scalafix AddImplicitNotFound;scalafix TypeClassSupport;"
    )
  )

lazy val integration = (project in file("integration"))
  .dependsOn(examples)
  .dependsOn(mockingbird)
  .dependsOn(`mockingbird-api`)
  .enablePlugins(
    JavaAppPackaging
  )
  .settings(Settings.common)
  .settings(
    publish / skip := true,
    Test / fork := true,
    libraryDependencies ++= Seq(
      Dependencies.scalatest,
      Dependencies.scalacheck,
    ).flatten,
    libraryDependencies ++= Seq(
      "com.dimafeng"  %% "testcontainers-scala" % "0.40.17" % Test,
    ),
    Test / javaOptions += "-Dconfig.resource=test.conf",
  )
  .settings(
    addCommandAlias(
      "fixCheck",
      "scalafixAll --check; scalafmtCheck"
    ),
    addCommandAlias(
      "lintAll",
      "scalafixAll; scalafmtAll"
    ),
    addCommandAlias(
      "simulacrum",
      "scalafixEnable;scalafix AddSerializable;scalafix AddImplicitNotFound;scalafix TypeClassSupport;"
    )
  )

val root = (project in file("."))
  .aggregate(
    utils,
    circeUtils,
    dataAccess,
    mockingbird,
    `mockingbird-api`,
    `mockingbird-native`,
    examples
  )
  .settings(
    run / aggregate := false,
  )
