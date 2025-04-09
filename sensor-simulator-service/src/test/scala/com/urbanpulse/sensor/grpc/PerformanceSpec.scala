package com.urbanpulse.sensor.grpc

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import com.urbanpulse.traffic._
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.grpc.Predef._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class PerformanceSpec extends BaseGrpcTest {
  private val grpcConfig = grpc(ManagedChannelBuilder.forAddress("localhost", grpcPort).usePlaintext())

  private val trafficDataRequest = TrafficDataRequest(area = "TestArea", maxSensors = 10)
  private val sensorStatusRequest = SensorStatusRequest(sensorId = "test-sensor-1")
  private val alertRequest = AlertRequest(area = "TestArea", severityThreshold = 100)

  private val trafficDataScenario: ScenarioBuilder = scenario("Stream Traffic Data")
    .exec(
      grpc("Stream Traffic Data")
        .rpc(TrafficMonitoringServiceGrpc.METHOD_STREAM_TRAFFIC_DATA)
        .payload(trafficDataRequest)
        .extract(_.take(10))
    )

  private val sensorStatusScenario: ScenarioBuilder = scenario("Get Sensor Status")
    .exec(
      grpc("Get Sensor Status")
        .rpc(TrafficMonitoringServiceGrpc.METHOD_GET_SENSOR_STATUS)
        .payload(sensorStatusRequest)
    )

  private val alertsScenario: ScenarioBuilder = scenario("Stream Alerts")
    .exec(
      grpc("Stream Alerts")
        .rpc(TrafficMonitoringServiceGrpc.METHOD_GET_ACTIVE_ALERTS)
        .payload(alertRequest)
        .extract(_.take(5))
    )

  "gRPC Service Performance" should {
    "handle traffic data streaming under load" in {
      setUp(
        trafficDataScenario.inject(
          rampUsers(10) during (5.seconds),
          constantUsersPerSec(20) during (30.seconds)
        )
      ).protocols(grpcConfig)
        .assertions(
          global.responseTime.max.lt(1000),
          global.successfulRequests.percent.gt(99)
        )
    }

    "handle sensor status requests under load" in {
      setUp(
        sensorStatusScenario.inject(
          rampUsers(50) during (5.seconds),
          constantUsersPerSec(100) during (30.seconds)
        )
      ).protocols(grpcConfig)
        .assertions(
          global.responseTime.max.lt(500),
          global.successfulRequests.percent.gt(99)
        )
    }

    "handle alert streaming under load" in {
      setUp(
        alertsScenario.inject(
          rampUsers(5) during (5.seconds),
          constantUsersPerSec(10) during (30.seconds)
        )
      ).protocols(grpcConfig)
        .assertions(
          global.responseTime.max.lt(2000),
          global.successfulRequests.percent.gt(99)
        )
    }
  }
} 