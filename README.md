# UrbanPulse 🚦

**UrbanPulse** is a real-time traffic monitoring and emergency response microservices system built with Java 21 and Quarkus. It simulates urban traffic data, processes live events, detects congestion, and triggers emergency response actions using a fully asynchronous event-driven architecture powered by Apache Kafka.

---

## 🧱 Architecture Pattern

**Event-Driven Choreography**
- Services are loosely coupled
- Communication is handled via Kafka topics
- Each service reacts to relevant events and emits its own domain events

---

## 🧩 Microservices

### 1. `sensor-simulator-service`
- Simulates traffic sensors with high-frequency data (Gatling-style)
- Emits to Kafka topic: `traffic-sensor-data`

### 2. `citizen-report-service`
- REST API to submit accident reports
- Emits to Kafka topic: `accident-reports`

### 3. `traffic-analysis-service`
- Kafka Streams app
- Listens to `traffic-sensor-data`
- Emits alerts to `congestion-alerts` if congestion detected

### 4. `incident-coordinator-service`
- Listens to `accident-reports` and `congestion-alerts`
- Issues emergency commands via `incident-commands`

### 5. `alert-notifier-service`
- Listens to `incident-commands`
- Sends mocked alerts (console, logs, webhook)
- Emits to Kafka topic: `alert-notifications`

---

## 🔄 Kafka Topics

| Topic Name            | Description                                |
|-----------------------|--------------------------------------------|
| `traffic-sensor-data` | Sensor data stream from simulator           |
| `accident-reports`    | Reports submitted by citizens               |
| `congestion-alerts`   | Events emitted when congestion is detected  |
| `incident-commands`   | Commands for emergency responders           |
| `alert-notifications` | Notifications sent by the alert service     |

---

## 🛠 Tech Stack

- **Java 21**
- **Quarkus**
- **Apache Kafka**
- **Protobuf** (for event serialization)
- **Gatling** (for traffic simulation)
- **RESTEasy Reactive** (for REST APIs)
- **Docker & Docker Compose**

---

## 📦 Project Structure

```
urbanpulse/
├── shared-proto/                  # Protobuf schemas
├── shared-lib/                    # DTOs, utils
├── sensor-simulator-service/      # Simulated traffic stream
├── citizen-report-service/        # Public REST API
├── traffic-analysis-service/      # Kafka Streams logic
├── incident-coordinator-service/  # Emergency logic
├── alert-notifier-service/        # Sends alerts
├── docker/                        # Kafka, Zookeeper, Kafka UI
└── README.md
```

---

## 🚧 Roadmap

- [x] Define architecture and tech stack
- [ ] Implement shared Protobuf schemas
- [ ] Build Kafka Docker environment
- [ ] Develop sensor simulator service
- [ ] Create citizen report REST API
- [ ] Add Kafka Streams congestion analysis
- [ ] Coordinate incidents and alerts

---

## 👨‍💻 Author

Benan Aktas  
*Java Developer | Microservices Architect | Traffic Tech Enthusiast*
