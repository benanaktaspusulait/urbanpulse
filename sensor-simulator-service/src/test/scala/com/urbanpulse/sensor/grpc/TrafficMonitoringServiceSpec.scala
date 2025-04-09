package com.urbanpulse.sensor.grpc

import com.urbanpulse.traffic._
import com.urbanpulse.sensor.TrafficSensorSimulation
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import java.util.Properties
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class TrafficMonitoringServiceSpec extends BaseGrpcTest {
  private var channel: ManagedChannel = _
  private var client: TrafficMonitoringServiceGrpc.TrafficMonitoringServiceStub = _
  private var kafkaProducer: KafkaProducer[String, Array[Byte]] = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    
    // Start gRPC server
    val server = new GrpcServer
    server.start(grpcPort)

    // Create gRPC client
    channel = ManagedChannelBuilder.forAddress("localhost", grpcPort)
      .usePlaintext()
      .build()
    client = TrafficMonitoringServiceGrpc.newStub(channel)

    // Setup Kafka producer
    val kafkaProps = new Properties()
    kafkaProps.put("bootstrap.servers", s"localhost:${kafkaConfig.kafkaPort}")
    kafkaProps.put("key.serializer", classOf[StringSerializer].getName)
    kafkaProps.put("value.serializer", classOf[ByteArraySerializer].getName)
    kafkaProducer = new KafkaProducer[String, Array[Byte]](kafkaProps)
  }

  override def afterAll(): Unit = {
    channel.shutdown()
    kafkaProducer.close()
    super.afterAll()
  }

  "TrafficMonitoringService" should {
    "stream traffic data" in {
      val request = TrafficDataRequest(area = "TestArea", maxSensors = 2)
      val responseObserver = new TestStreamObserver[TrafficSensorData]
      
      client.streamTrafficData(request, responseObserver)
      
      // Publish test data
      val testData = TrafficSensorData(
        sensorId = "test-sensor-1",
        latitude = 41.0082,
        longitude = 28.9784,
        vehicleCount = 150,
        timestamp = System.currentTimeMillis()
      )
      kafkaProducer.send(new ProducerRecord("traffic-sensor-data", testData.sensorId, testData.toByteArray))
      
      whenReady(responseObserver.completionFuture) { _ =>
        responseObserver.receivedMessages should have size 1
        val receivedData = responseObserver.receivedMessages.head
        receivedData.sensorId shouldBe "test-sensor-1"
        receivedData.vehicleCount shouldBe 150
      }
    }

    "get sensor status" in {
      val request = SensorStatusRequest(sensorId = "test-sensor-1")
      val responseObserver = new TestStreamObserver[SensorStatus]
      
      client.getSensorStatus(request, responseObserver)
      
      // Publish test status
      val testStatus = SensorStatus(
        sensorId = "test-sensor-1",
        status = "online",
        timestamp = System.currentTimeMillis()
      )
      kafkaProducer.send(new ProducerRecord("sensor-status", testStatus.sensorId, testStatus.toByteArray))
      
      whenReady(responseObserver.completionFuture) { _ =>
        responseObserver.receivedMessages should have size 1
        val receivedStatus = responseObserver.receivedMessages.head
        receivedStatus.sensorId shouldBe "test-sensor-1"
        receivedStatus.status shouldBe "online"
      }
    }

    "report accidents" in {
      val request = AccidentReport(
        reportId = "test-accident-1",
        reporterName = "Test Reporter",
        latitude = 41.0082,
        longitude = 28.9784,
        description = "Test accident",
        timestamp = System.currentTimeMillis()
      )
      val responseObserver = new TestStreamObserver[AccidentReportResponse]
      
      client.reportAccident(request, responseObserver)
      
      whenReady(responseObserver.completionFuture) { _ =>
        responseObserver.receivedMessages should have size 1
        val response = responseObserver.receivedMessages.head
        response.reportId shouldBe "test-accident-1"
        response.success shouldBe true
      }
    }

    "stream active alerts" in {
      val request = AlertRequest(area = "TestArea", severityThreshold = 100)
      val responseObserver = new TestStreamObserver[CongestionAlert]
      
      client.getActiveAlerts(request, responseObserver)
      
      // Publish test alert
      val testAlert = CongestionAlert(
        alertId = "test-alert-1",
        sensorId = "test-sensor-1",
        latitude = 41.0082,
        longitude = 28.9784,
        vehicleCount = 200,
        timestamp = System.currentTimeMillis()
      )
      kafkaProducer.send(new ProducerRecord("congestion-alerts", testAlert.alertId, testAlert.toByteArray))
      
      whenReady(responseObserver.completionFuture) { _ =>
        responseObserver.receivedMessages should have size 1
        val receivedAlert = responseObserver.receivedMessages.head
        receivedAlert.alertId shouldBe "test-alert-1"
        receivedAlert.vehicleCount shouldBe 200
      }
    }
  }
}

class TestStreamObserver[T] extends io.grpc.stub.StreamObserver[T] {
  private val messages = scala.collection.mutable.ListBuffer[T]()
  private val completionPromise = scala.concurrent.Promise[Unit]()
  
  def receivedMessages: List[T] = messages.toList
  def completionFuture = completionPromise.future
  
  override def onNext(value: T): Unit = messages += value
  override def onError(t: Throwable): Unit = completionPromise.failure(t)
  override def onCompleted(): Unit = completionPromise.success(())
} 