package com.urbanpulse.sensor

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.urbanpulse.sensor.dashboard.Dashboard
import com.urbanpulse.sensor.grpc.GrpcServer
import com.typesafe.config.ConfigFactory
import play.api.libs.ws.ahc.AhcWSClient
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("urbanpulse")
  implicit val materializer: Materializer = Materializer(system)
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val ws: AhcWSClient = AhcWSClient()

  // Start the dashboard
  val dashboard = new Dashboard
  dashboard.start()

  // Start the gRPC server
  GrpcServer.start()

  // Start the traffic simulation
  TrafficSensorSimulation.startSimulation()

  // Add shutdown hook
  sys.addShutdownHook {
    println("Shutting down...")
    ws.close()
    system.terminate()
  }
} 