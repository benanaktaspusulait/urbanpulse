name := "sensor-simulator-service"
version := "1.0-SNAPSHOT"
scalaVersion := "2.12.18"

val gatlingVersion = "3.9.5"
val kafkaVersion = "3.5.1"
val grpcVersion = "1.58.0"
val protobufVersion = "3.24.4"
val akkaVersion = "2.6.20"
val akkaHttpVersion = "10.2.10"
val playVersion = "2.8.20"
val scalatestVersion = "3.2.15"
val embeddedKafkaVersion = "3.5.1"

libraryDependencies ++= Seq(
  // Scala
  "org.scala-lang" % "scala-library" % scalaVersion.value,

  // Gatling
  "io.gatling" % "gatling-core" % gatlingVersion,
  "io.gatling" % "gatling-http" % gatlingVersion,

  // Kafka
  "org.apache.kafka" % "kafka-clients" % kafkaVersion,
  "io.github.embeddedkafka" %% "embedded-kafka" % embeddedKafkaVersion % Test,

  // gRPC
  "io.grpc" % "grpc-netty" % grpcVersion,
  "io.grpc" % "grpc-protobuf" % grpcVersion,
  "io.grpc" % "grpc-stub" % grpcVersion,

  // Protobuf
  "com.google.protobuf" % "protobuf-java" % protobufVersion,

  // Shared Proto (assuming it's published locally)
  "com.urbanpulse" % "shared-proto" % "1.0-SNAPSHOT",

  // Akka
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  
  // Play Framework
  "com.typesafe.play" %% "play" % playVersion,
  "com.typesafe.play" %% "play-akka-http-server" % playVersion,
  "com.typesafe.play" %% "play-json" % playVersion,
  
  // Testing
  "org.scalatest" %% "scalatest" % scalatestVersion % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "io.grpc" % "grpc-testing" % grpcVersion % Test,
  "com.github.tomakehurst" % "wiremock" % "2.27.2" % Test
)

// Enable Gatling plugin
enablePlugins(GatlingPlugin)

// Protobuf settings
PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

// Add protobuf dependencies
libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"

// Gatling settings
scalaSource in Gatling := sourceDirectory.value / "test" / "scala"
resourceDirectory in Gatling := sourceDirectory.value / "test" / "resources"

// Assembly settings for creating a fat JAR
assembly / assemblyJarName := "sensor-simulator-service.jar"
assembly / mainClass := Some("com.urbanpulse.sensor.Main")
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

// Test settings
Test / fork := true
Test / javaOptions += s"-Dconfig.file=${baseDirectory.value}/src/test/resources/application.conf" 