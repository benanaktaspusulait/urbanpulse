package com.urbanpulse.sensor.grpc

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import com.urbanpulse.sensor.TrafficSensorSimulation
import io.github.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

abstract class BaseGrpcTest extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures with EmbeddedKafka {
  implicit val system: ActorSystem = ActorSystem("test-system")
  implicit val materializer: Materializer = Materializer(system)
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 100.millis)

  protected val config = ConfigFactory.load()
  protected val grpcPort = config.getInt("grpc.port")
  
  // Kafka test configuration
  protected implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(
    kafkaPort = 9092,
    zooKeeperPort = 2181
  )

  override def beforeAll(): Unit = {
    // Start embedded Kafka
    EmbeddedKafka.start()
    
    // Create required topics
    createCustomTopic("traffic-sensor-data")
    createCustomTopic("sensor-status")
    createCustomTopic("congestion-alerts")
    createCustomTopic("accident-reports")
    
    // Initialize test data
    TrafficSensorSimulation.initializeTestData()
  }

  override def afterAll(): Unit = {
    EmbeddedKafka.stop()
    system.terminate()
  }
} 