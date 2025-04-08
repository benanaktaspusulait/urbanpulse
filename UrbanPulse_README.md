
# UrbanPulse 🚦

**UrbanPulse** is a real-time traffic monitoring and emergency response system designed using a modern microservice-based architecture. It is built with **Java 21**, **Quarkus**, **Apache Kafka**, **Protobuf**, and **Docker**, and it includes high-throughput traffic data simulation using **Gatling**.

---

## 🧠 Project Goals

- Simulate urban traffic activity using virtual sensors
- Collect real-time accident reports from users or third-party apps
- Analyze traffic conditions and detect congestion in real-time
- Automatically coordinate emergency responses based on events
- Deliver alerts and system-level commands in response to incidents

---

## 🧱 Architecture Pattern

### ✅ Event-Driven Choreography

- Services are loosely coupled and communicate via Kafka
- Each service listens to relevant topics and publishes domain events
- No central orchestrator — logic is distributed across services

---

## ⚙️ Tech Stack

| Layer                      | Technology                         |
|---------------------------|-------------------------------------|
| Language                  | Java 21                             |
| Framework                 | Quarkus                             |
| Messaging                 | Apache Kafka                        |
| Serialization             | Protobuf (Kafka messages)           |
| External Communication    | REST (Quarkus RESTEasy Reactive)    |
| Simulation & Load         | Gatling (within sensor service)     |
| Containerization          | Docker & Docker Compose             |
| Build Tool                | Maven (multi-module)                |

---

## 🧩 Microservices

### `sensor-simulator-service`
- Simulates high-throughput traffic sensor data
- Publishes to Kafka topic: `traffic-sensor-data`
- Internally runs Gatling-style simulation logic

### `citizen-report-service`
- Exposes REST endpoint to receive accident reports
- Publishes to Kafka topic: `accident-reports`

### `traffic-analysis-service`
- Kafka Streams-based microservice
- Listens to `traffic-sensor-data` and detects congestion
- Publishes to Kafka topic: `congestion-alerts`

### `incident-coordinator-service`
- Listens to `accident-reports` and `congestion-alerts`
- Cross-analyzes incidents and triggers emergency response logic
- Publishes to Kafka topic: `incident-commands`

### `alert-notifier-service`
- Listens to `incident-commands`
- Sends out alerts (mocked via logs, console, or future integrations)

---

## 🔁 Kafka Topics

| Topic Name            | Description                                    |
|----------------------|------------------------------------------------|
| `traffic-sensor-data` | Traffic sensor readings from simulation        |
| `accident-reports`    | Reports of incidents submitted via REST API    |
| `congestion-alerts`   | Alerts emitted by the traffic analysis service |
| `incident-commands`   | Commands to trigger emergency responses        |

---

## 📦 Project Structure

```
urbanpulse/
├── shared-proto/                    # Protobuf message definitions
├── shared-lib/                      # Common utilities and DTOs
├── sensor-simulator-service/        # Traffic data producer (Gatling simulation)
├── citizen-report-service/          # External REST API for incident reports
├── traffic-analysis-service/        # Kafka Streams congestion detection
├── incident-coordinator-service/    # Emergency logic service
├── alert-notifier-service/          # Alert logger/mock dispatcher
├── docker/                          # Kafka, Zookeeper, Kafka UI stack
└── README.md
```

---

## 📊 System Flow

1. `sensor-simulator-service` streams simulated traffic data to Kafka.
2. `citizen-report-service` exposes a public REST API and emits accident reports.
3. `traffic-analysis-service` processes sensor data in real-time and detects congestion.
4. `incident-coordinator-service` listens to both reports and congestion alerts, determines severity, and emits commands.
5. `alert-notifier-service` consumes commands and logs or mocks alerts.

---

## 🐳 Dockerized Environment

A full **Docker Compose** setup includes:
- Kafka & Zookeeper
- Kafka UI (for monitoring)
- Optional Schema Registry

Each microservice runs in its own container and communicates via Kafka.

---

## 🚀 How to Run (Coming Soon)

Instructions for:
- Building the project with Maven
- Running each service with Docker
- Publishing Protobuf messages to Kafka
- Accessing REST endpoints
- Viewing messages in Kafka UI

---

## 🛣️ Roadmap

- [x] Architecture finalized
- [x] Tech stack defined
- [ ] Protobuf schemas
- [ ] Initial service implementations
- [ ] Kafka Docker environment
- [ ] Load simulation and testing

---

## 👨‍💻 Author

**Benan Aktas**  
*Senior Java Developer | Cloud-Native Architect | Microservices Expert*

---

## 📄 License

MIT License (or custom, if preferred)

---
