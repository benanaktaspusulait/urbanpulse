package com.urbanpulse.sensor.grpc

import akka.actor.ActorSystem
import akka.stream.Materializer
import io.grpc.ServerBuilder
import scala.concurrent.ExecutionContext

object GrpcServer {
  def start(port: Int = 50051)(implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext): Unit = {
    val server = ServerBuilder
      .forPort(port)
      .addService(new TrafficMonitoringServiceImpl)
      .build()
      .start()

    println(s"gRPC server started, listening on port $port")

    // Add shutdown hook
    sys.addShutdownHook {
      System.err.println("Shutting down gRPC server")
      server.shutdown()
    }

    server.awaitTermination()
  }
} 