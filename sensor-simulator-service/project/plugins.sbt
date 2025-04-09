// Gatling plugin
addSbtPlugin("io.gatling" % "gatling-sbt" % "3.9.5")

// Assembly plugin for creating fat JARs
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1")

// ScalaPB plugin for Protocol Buffers
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")

// ScalaPB runtime
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.13" 