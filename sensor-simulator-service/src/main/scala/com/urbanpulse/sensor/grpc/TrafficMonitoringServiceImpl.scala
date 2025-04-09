package com.urbanpulse.sensor.grpc

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import com.urbanpulse.traffic._
import com.urbanpulse.sensor.TrafficSensorSimulation
import io.grpc.stub.StreamObserver
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import play.api.libs.json.Json

import java.util.Properties
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

class TrafficMonitoringServiceImpl(implicit system: ActorSystem, ec: ExecutionContext) 
    extends TrafficMonitoringServiceGrpc.TrafficMonitoringServiceImplBase {

  private val kafkaProps = new Properties()
  kafkaProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  kafkaProps.put(ConsumerConfig.GROUP_ID_CONFIG, "grpc-service")
  kafkaProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])
  kafkaProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[ByteArrayDeserializer])
  kafkaProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  override def streamTrafficData(
      request: TrafficDataRequest,
      responseObserver: StreamObserver[TrafficSensorData]
  ): Unit = {
    val consumer = new KafkaConsumer[String, Array[Byte]](kafkaProps)
    consumer.subscribe(java.util.Collections.singletonList("traffic-sensor-data"))

    Source.fromIterator(() => consumer.poll(java.time.Duration.ofMillis(100)).iterator().asScala)
      .map(record => TrafficSensorData.parseFrom(record.value()))
      .filter(data => request.area.isEmpty || TrafficSensorSimulation.getAreaForCoordinates(data.latitude, data.longitude).name == request.area)
      .take(request.maxSensors)
      .runWith(Sink.foreach(data => responseObserver.onNext(data)))
      .onComplete(_ => {
        consumer.close()
        responseObserver.onCompleted()
      })
  }

  override def getSensorStatus(
      request: SensorStatusRequest,
      responseObserver: StreamObserver[SensorStatus]
  ): Unit = {
    val consumer = new KafkaConsumer[String, Array[Byte]](kafkaProps)
    consumer.subscribe(java.util.Collections.singletonList("sensor-status"))

    Source.fromIterator(() => consumer.poll(java.time.Duration.ofMillis(100)).iterator().asScala)
      .map(record => SensorStatus.parseFrom(record.value()))
      .filter(status => request.sensorId.isEmpty || status.sensorId == request.sensorId)
      .take(1)
      .runWith(Sink.foreach(status => {
        responseObserver.onNext(status)
        responseObserver.onCompleted()
      }))
      .onComplete(_ => consumer.close())
  }

  override def reportAccident(
      request: AccidentReport,
      responseObserver: StreamObserver[AccidentReportResponse]
  ): Unit = {
    // Publish to Kafka topic
    TrafficSensorSimulation.publishAccidentReport(request)
    
    responseObserver.onNext(AccidentReportResponse(
      reportId = request.reportId,
      success = true,
      message = "Accident report received and published"
    ))
    responseObserver.onCompleted()
  }

  override def getActiveAlerts(
      request: AlertRequest,
      responseObserver: StreamObserver[CongestionAlert]
  ): Unit = {
    val consumer = new KafkaConsumer[String, Array[Byte]](kafkaProps)
    consumer.subscribe(java.util.Collections.singletonList("congestion-alerts"))

    Source.fromIterator(() => consumer.poll(java.time.Duration.ofMillis(100)).iterator().asScala)
      .map(record => CongestionAlert.parseFrom(record.value()))
      .filter(alert => {
        val matchesArea = request.area.isEmpty || 
          TrafficSensorSimulation.getAreaForCoordinates(alert.latitude, alert.longitude).name == request.area
        val matchesSeverity = alert.vehicleCount >= request.severityThreshold
        matchesArea && matchesSeverity
      })
      .runWith(Sink.foreach(alert => responseObserver.onNext(alert)))
      .onComplete(_ => {
        consumer.close()
        responseObserver.onCompleted()
      })
  }
} 