package com.urbanpulse.sensor.dashboard

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import play.api.libs.json._
import play.api.mvc._
import play.api.routing.sird._
import play.api.routing.Router
import play.core.server.{NettyServer, ServerConfig}
import com.urbanpulse.{TrafficSensorData, SensorStatus, CongestionAlert, AccidentReport}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class Dashboard(implicit system: ActorSystem, ec: ExecutionContext) {
  // Kafka consumer configuration
  val consumerProps = Map(
    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> "kafka:9092",
    ConsumerConfig.GROUP_ID_CONFIG -> "dashboard-consumer",
    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer].getName,
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG -> classOf[ByteArrayDeserializer].getName,
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "latest"
  )

  // Store recent data for visualization
  val recentTraffic = scala.collection.mutable.Map[String, TrafficSensorData]()
  val recentStatus = scala.collection.mutable.Map[String, SensorStatus]()
  val recentAlerts = scala.collection.mutable.ArrayBuffer[CongestionAlert]()
  val recentAccidents = scala.collection.mutable.ArrayBuffer[AccidentReport]()

  // Create Kafka consumer
  val consumer = new org.apache.kafka.clients.consumer.KafkaConsumer[String, Array[Byte]](consumerProps)
  consumer.subscribe(java.util.Arrays.asList("traffic-sensor-data", "sensor-status", "congestion-alerts", "accident-reports"))

  // Start consuming messages
  Source.fromIterator(() => consumer.poll(java.time.Duration.ofMillis(100)).iterator())
    .runWith(Sink.foreach(handleMessage))

  def handleMessage(record: ConsumerRecord[String, Array[Byte]]): Unit = {
    record.topic() match {
      case "traffic-sensor-data" =>
        val data = TrafficSensorData.parseFrom(record.value())
        recentTraffic(data.getSensorId) = data
      case "sensor-status" =>
        val status = SensorStatus.parseFrom(record.value())
        recentStatus(status.getSensorId) = status
      case "congestion-alerts" =>
        val alert = CongestionAlert.parseFrom(record.value())
        recentAlerts += alert
        if (recentAlerts.length > 100) recentAlerts.remove(0)
      case "accident-reports" =>
        val accident = AccidentReport.parseFrom(record.value())
        recentAccidents += accident
        if (recentAccidents.length > 50) recentAccidents.remove(0)
    }
  }

  // Create router for HTTP endpoints
  val router = Router.from {
    case GET(p"/api/traffic") =>
      Action {
        Ok(Json.toJson(recentTraffic.values.map(data => Json.obj(
          "sensorId" -> data.getSensorId,
          "latitude" -> data.getLatitude,
          "longitude" -> data.getLongitude,
          "vehicleCount" -> data.getVehicleCount,
          "timestamp" -> data.getTimestamp
        ))))
      }
    case GET(p"/api/status") =>
      Action {
        Ok(Json.toJson(recentStatus.values.map(status => Json.obj(
          "sensorId" -> status.getSensorId,
          "status" -> status.getStatus,
          "timestamp" -> status.getTimestamp
        ))))
      }
    case GET(p"/api/alerts") =>
      Action {
        Ok(Json.toJson(recentAlerts.map(alert => Json.obj(
          "alertId" -> alert.getAlertId,
          "sensorId" -> alert.getSensorId,
          "latitude" -> alert.getLatitude,
          "longitude" -> alert.getLongitude,
          "vehicleCount" -> alert.getVehicleCount,
          "timestamp" -> alert.getTimestamp
        ))))
      }
    case GET(p"/api/accidents") =>
      Action {
        Ok(Json.toJson(recentAccidents.map(accident => Json.obj(
          "reportId" -> accident.getReportId,
          "reporterName" -> accident.getReporterName,
          "latitude" -> accident.getLatitude,
          "longitude" -> accident.getLongitude,
          "description" -> accident.getDescription,
          "timestamp" -> accident.getTimestamp
        ))))
      }
    case GET(p"/") =>
      Action {
        Ok(views.html.index())
      }
  }
}

object Dashboard {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("dashboard")
    implicit val ec = system.dispatcher

    val dashboard = new Dashboard()
    val server = NettyServer.fromRouter(ServerConfig(port = Some(9001)))(dashboard.router)
  }
} 