package com.urbanpulse.sensor

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import com.urbanpulse.{TrafficSensorData, SensorStatus, CongestionAlert, AccidentReport}
import java.util.concurrent.ThreadLocalRandom
import scala.concurrent.duration._
import scala.collection.mutable.{Map => MutableMap}

class TrafficSensorSimulation extends Simulation {
  // Configuration
  val trafficTopic = "traffic-sensor-data"
  val statusTopic = "sensor-status"
  val congestionTopic = "congestion-alerts"
  val accidentTopic = "accident-reports"
  
  // Istanbul major areas and their traffic patterns
  case class Area(name: String, centerLat: Double, centerLon: Double, radius: Double, 
                 baseTraffic: Int, peakMultiplier: Double, rushHours: List[(Int, Int)])
  
  val areas = List(
    Area("Taksim", 41.0370, 28.9850, 0.02, 300, 2.5, List((8, 10), (17, 19))),
    Area("Kadikoy", 40.9927, 29.0284, 0.02, 250, 2.0, List((8, 10), (17, 19))),
    Area("Besiktas", 41.0422, 29.0089, 0.02, 200, 2.0, List((8, 10), (17, 19))),
    Area("Uskudar", 41.0225, 29.0236, 0.02, 180, 1.8, List((8, 10), (17, 19))),
    Area("Sisli", 41.0602, 28.9877, 0.02, 220, 2.2, List((8, 10), (17, 19))),
    Area("Residential", 41.0082, 28.9784, 0.1, 50, 1.5, List((8, 9), (17, 18)))
  )

  // Track sensor states and recent events
  val sensorStates = MutableMap[String, (Double, Double, Int)]()
  val recentAccidents = MutableMap[String, Long]() // sensorId -> timestamp
  val recentCongestions = MutableMap[String, Long]() // sensorId -> timestamp

  // Helper function to get current hour
  def getCurrentHour: Int = {
    (System.currentTimeMillis() / 3600000) % 24
  }

  // Helper function to check if it's rush hour
  def isRushHour(area: Area): Boolean = {
    val currentHour = getCurrentHour
    area.rushHours.exists { case (start, end) => currentHour >= start && currentHour < end }
  }

  // Helper function to generate coordinates within an area
  def randomCoordinateInArea(area: Area): (Double, Double) = {
    val angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI
    val radius = ThreadLocalRandom.current().nextDouble() * area.radius
    val lat = area.centerLat + radius * Math.sin(angle)
    val lon = area.centerLon + radius * Math.cos(angle)
    (lat, lon)
  }

  // Helper function to calculate traffic multiplier
  def calculateTrafficMultiplier(area: Area, sensorId: String): Double = {
    var multiplier = 1.0
    
    // Rush hour effect
    if (isRushHour(area)) {
      multiplier *= area.peakMultiplier
    }
    
    // Recent accident effect (last 30 minutes)
    recentAccidents.get(sensorId).foreach { timestamp =>
      if (System.currentTimeMillis() - timestamp < 1800000) {
        multiplier *= 1.5
      }
    }
    
    // Recent congestion effect (last 15 minutes)
    recentCongestions.get(sensorId).foreach { timestamp =>
      if (System.currentTimeMillis() - timestamp < 900000) {
        multiplier *= 1.3
      }
    }
    
    // Random variation
    multiplier *= (0.9 + ThreadLocalRandom.current().nextDouble() * 0.2)
    
    multiplier
  }

  // Create traffic sensor data with realistic patterns
  def createTrafficData(sensorId: String): TrafficSensorData = {
    val area = areas(ThreadLocalRandom.current().nextInt(areas.length))
    val (lat, lon) = randomCoordinateInArea(area)
    val baseCount = area.baseTraffic
    val multiplier = calculateTrafficMultiplier(area, sensorId)
    val vehicleCount = (baseCount * multiplier).toInt
    
    // Update sensor state
    sensorStates(sensorId) = (lat, lon, vehicleCount)
    
    TrafficSensorData.newBuilder()
      .setSensorId(sensorId)
      .setLatitude(lat)
      .setLongitude(lon)
      .setVehicleCount(vehicleCount)
      .setTimestamp(System.currentTimeMillis())
      .build()
  }

  // Create sensor status with correlated failures
  def createSensorStatus(sensorId: String): SensorStatus = {
    val (_, _, vehicleCount) = sensorStates.getOrElse(sensorId, (0.0, 0.0, 0))
    val baseFailureRate = 0.05
    
    // Increase failure rate during high traffic
    val failureRate = if (vehicleCount > 800) baseFailureRate * 2 else baseFailureRate
    
    val status = if (ThreadLocalRandom.current().nextDouble() < failureRate) "offline" else "online"
    
    SensorStatus.newBuilder()
      .setSensorId(sensorId)
      .setStatus(status)
      .setTimestamp(System.currentTimeMillis())
      .build()
  }

  // Create congestion alert with area-specific thresholds
  def createCongestionAlert(sensorId: String, vehicleCount: Int): CongestionAlert = {
    val (lat, lon, _) = sensorStates(sensorId)
    val area = areas.find(a => 
      Math.sqrt(Math.pow(lat - a.centerLat, 2) + Math.pow(lon - a.centerLon, 2)) < a.radius
    ).getOrElse(areas.head)
    
    val threshold = area.baseTraffic * area.peakMultiplier * 0.8
    
    if (vehicleCount > threshold) {
      recentCongestions(sensorId) = System.currentTimeMillis()
      
      CongestionAlert.newBuilder()
        .setAlertId(java.util.UUID.randomUUID().toString)
        .setSensorId(sensorId)
        .setLatitude(lat)
        .setLongitude(lon)
        .setVehicleCount(vehicleCount)
        .setTimestamp(System.currentTimeMillis())
        .build()
    } else null
  }

  // Create accident report with area-specific probabilities
  def createAccidentReport(): AccidentReport = {
    val reporters = List("John Doe", "Jane Smith", "Mike Johnson", "Sarah Williams", "David Brown")
    val descriptions = List(
      "Car accident with minor injuries",
      "Multiple vehicle collision",
      "Pedestrian involved accident",
      "Motorcycle accident",
      "Truck collision"
    )
    
    // Higher accident probability during rush hours and in high-traffic areas
    val area = areas(ThreadLocalRandom.current().nextInt(areas.length))
    val baseProbability = 0.05
    val rushHourMultiplier = if (isRushHour(area)) 2.0 else 1.0
    val trafficMultiplier = if (area.baseTraffic > 200) 1.5 else 1.0
    val probability = baseProbability * rushHourMultiplier * trafficMultiplier
    
    if (ThreadLocalRandom.current().nextDouble() < probability) {
      val (lat, lon) = randomCoordinateInArea(area)
      val report = AccidentReport.newBuilder()
        .setReportId(java.util.UUID.randomUUID().toString)
        .setReporterName(reporters(ThreadLocalRandom.current().nextInt(reporters.length)))
        .setLatitude(lat)
        .setLongitude(lon)
        .setDescription(descriptions(ThreadLocalRandom.current().nextInt(descriptions.length)))
        .setTimestamp(System.currentTimeMillis())
        .build()
      
      // Find nearest sensor and mark it for traffic impact
      val nearestSensor = sensorStates.minBy { case (_, (sLat, sLon, _)) =>
        Math.sqrt(Math.pow(lat - sLat, 2) + Math.pow(lon - sLon, 2))
      }._1
      recentAccidents(nearestSensor) = System.currentTimeMillis()
      
      report
    } else null
  }

  // Traffic sensor scenario
  val trafficScenario: ScenarioBuilder = scenario("Traffic Sensor Simulation")
    .exec(session => {
      val sensorId = s"sensor-${ThreadLocalRandom.current().nextInt(1000)}"
      val trafficData = createTrafficData(sensorId)
      val vehicleCount = trafficData.getVehicleCount
      
      // Publish traffic data
      Main.producer.send(new ProducerRecord[String, Array[Byte]](
        trafficTopic,
        sensorId,
        trafficData.toByteArray
      ))

      // Publish sensor status
      val statusData = createSensorStatus(sensorId)
      Main.producer.send(new ProducerRecord[String, Array[Byte]](
        statusTopic,
        sensorId,
        statusData.toByteArray
      ))

      // Publish congestion alert if applicable
      val congestionData = createCongestionAlert(sensorId, vehicleCount)
      if (congestionData != null) {
        Main.producer.send(new ProducerRecord[String, Array[Byte]](
          congestionTopic,
          sensorId,
          congestionData.toByteArray
        ))
      }
      
      session
    })

  // Accident report scenario
  val accidentScenario: ScenarioBuilder = scenario("Accident Report Simulation")
    .exec(session => {
      val accidentData = createAccidentReport()
      if (accidentData != null) {
        Main.producer.send(new ProducerRecord[String, Array[Byte]](
          accidentTopic,
          accidentData.getReportId,
          accidentData.toByteArray
        ))
      }
      session
    })

  // Simulation setup
  setUp(
    trafficScenario
      .inject(
        constantUsersPerSec(10) during (1 minute),  // Ramp up
        constantUsersPerSec(50) during (5 minutes), // Steady state
        constantUsersPerSec(10) during (1 minute)   // Ramp down
      ),
    accidentScenario
      .inject(
        constantUsersPerSec(1) during (7 minutes)  // Continuous low rate
      )
  ).protocols(http)
    .maxDuration(7 minutes)
} 