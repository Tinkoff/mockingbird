addDependencyTreePlugin
addSbtPlugin("com.typesafe.sbt" % "sbt-git"             % "1.0.0")
addSbtPlugin("com.eed3si9n"     % "sbt-buildinfo"       % "0.10.0")
addSbtPlugin("com.github.sbt"   % "sbt-native-packager" % "1.9.9")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix"        % "0.9.31")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"        % "2.4.3")
addSbtPlugin("ch.epfl.scala"    % "sbt-missinglink"     % "0.3.3")
addSbtPlugin("com.thesamet"     % "sbt-protoc"          % "1.0.2")
addSbtPlugin("org.scalameta"    % "sbt-native-image"    % "0.3.1")

libraryDependencies +=
  "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % "0.6.0-test4"
