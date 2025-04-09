lazy val root = (project in file("."))
  .settings(
    name := "urbanpulse",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.12.18"
  )
  .aggregate(sensorSimulator)

lazy val sensorSimulator = (project in file("sensor-simulator-service"))
  .settings(
    name := "sensor-simulator-service",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.12.18"
  ) 