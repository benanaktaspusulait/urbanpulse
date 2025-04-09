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
| gRPC                      | gRPC (real-time communication)      |

### Sensor Simulator Service Tech Stack
| Component                 | Technology                         |
|---------------------------|-------------------------------------|
| Language                  | Scala 2.12.18                      |
| Build Tool                | SBT                                |
| Simulation Framework      | Gatling 3.9.5                      |
| Real-time Communication   | gRPC 1.58.0                        |
| Streaming                 | Akka Streams 2.6.20                |
| Web Framework             | Play Framework 2.8.20              |
| Testing                   | ScalaTest 3.2.15                   |
| Kafka Integration         | Kafka Clients 3.5.1                |
| Embedded Testing          | Embedded Kafka 3.5.1               |

---

## 🧩 Microservices

### `sensor-simulator-service`
- Simulates high-throughput traffic sensor data
- Publishes to Kafka topic: `traffic-sensor-data`
- Internally runs Gatling-style simulation logic
- Provides real-time gRPC streaming interface
- Features:
  - Realistic traffic pattern simulation
  - Geographic area-based traffic distribution
  - Sensor health monitoring
  - Performance testing capabilities
  - Interactive web dashboard

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

### `sensor-status-service`
- Publishes sensor status to Kafka topic: `sensor-status`

### `traffic-predictions-service`
- Publishes traffic predictions to Kafka topic: `traffic-predictions`

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

### `dashboard-service`
- Provides real-time traffic visualization
- Features:
  - Real-time traffic map
  - Sensor status monitoring
  - Alert visualization
  - Historical data analysis

---

## 🔁 Kafka Topics

| Topic Name            | Description                                    |
|----------------------|------------------------------------------------|
| `traffic-sensor-data` | Traffic sensor readings from simulation        |
| `accident-reports`    | Reports of incidents submitted via REST API    |
| `congestion-alerts`   | Alerts emitted by the traffic analysis service |
| `incident-commands`   | Commands to trigger emergency responses        |
| `sensor-status`       | Health status of sensors                        |
| `traffic-predictions` | Traffic flow predictions                        |

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

## 🚀 Kafka Setup and Deployment

### Prerequisites
- Kubernetes cluster (local or cloud-based)
- `kubectl` configured to access your cluster
- Docker installed (for local development)

### Deployment Steps

1. **Deploy Zookeeper**
   ```bash
   kubectl apply -f k8s/zookeeper-deployment.yaml
   kubectl wait --for=condition=ready pod -l app=zookeeper --timeout=300s
   ```

2. **Deploy Kafka**
   ```bash
   kubectl apply -f k8s/kafka-deployment.yaml
   kubectl wait --for=condition=ready pod -l app=kafka --timeout=300s
   ```

3. **Deploy Kafdrop (Kafka UI)**
   ```bash
   kubectl apply -f k8s/kafdrop-rbac.yaml
   kubectl apply -f k8s/kafdrop-deployment.yaml
   kubectl apply -f k8s/kafdrop-service.yaml
   ```

4. **Access Kafdrop UI**
   ```bash
   kubectl port-forward service/kafdrop 9000:9000
   ```
   Then open your browser at `http://localhost:9000`

### Kafka Configuration

The Kafka deployment uses the following key configurations:

- **Image**: `bitnami/kafka:3.5.1`
- **Zookeeper Connection**: `zookeeper:2181`
- **Listeners**: PLAINTEXT://:9092
- **Advertised Listeners**: PLAINTEXT://kafka:9092
- **Auto Topic Creation**: Enabled
- **Resources**:
  - CPU: 1000m request, no limit
  - Memory: 2Gi request, 4Gi limit
  - Storage: 10Gi request, 20Gi limit

### Kafka Topics

The system uses the following Kafka topics:

1. **traffic-sensor-data**
   - Purpose: Traffic sensor readings from simulation
   - Producer: `sensor-simulator-service`
   - Consumer: `traffic-analysis-service`

2. **accident-reports**
   - Purpose: Reports of incidents submitted via REST API
   - Producer: `citizen-report-service`
   - Consumer: `incident-coordinator-service`

3. **congestion-alerts**
   - Purpose: Alerts emitted by the traffic analysis service
   - Producer: `traffic-analysis-service`
   - Consumer: `incident-coordinator-service`

4. **incident-commands**
   - Purpose: Commands to trigger emergency responses
   - Producer: `incident-coordinator-service`
   - Consumer: `alert-notifier-service`

5. **sensor-status**
   - Purpose: Health status of sensors
   - Producer: `sensor-simulator-service`
   - Consumer: `traffic-analysis-service`

6. **traffic-predictions**
   - Purpose: Traffic flow predictions
   - Producer: `traffic-analysis-service`
   - Consumer: `traffic-predictions-service`

### Monitoring and Management

- **Kafdrop UI**: Accessible at `http://localhost:9000` after port forwarding
  - View topics and their configurations
  - Monitor message flow
  - Browse message contents
  - Create and manage topics

- **Kubernetes Monitoring**:
  ```bash
  # Check pod status
  kubectl get pods
  
  # View logs
  kubectl logs -l app=kafka
  kubectl logs -l app=zookeeper
  
  # Check pod details
  kubectl describe pod -l app=kafka
  ```

### Troubleshooting

1. **If pods are not starting**:
   - Check logs: `kubectl logs -l app=kafka`
   - Verify Zookeeper connection: `kubectl exec -it <kafka-pod> -- nc -zv zookeeper 2181`
   - Check resource limits: `kubectl describe pod -l app=kafka`

2. **If Kafdrop cannot connect to Kafka**:
   - Verify Kafka service is running: `kubectl get service kafka`
   - Check network policies if any are applied
   - Verify RBAC permissions: `kubectl get rolebinding kafka-rb`

3. **If topics are not being created**:
   - Verify auto topic creation is enabled
   - Check Kafka logs for any errors
   - Ensure producers have proper permissions

### Cleanup

To remove all Kafka-related resources:
```bash
kubectl delete deployment kafdrop --ignore-not-found
kubectl delete deployment kafka --ignore-not-found
kubectl delete deployment zookeeper --ignore-not-found

kubectl delete service kafdrop --ignore-not-found
kubectl delete service kafka --ignore-not-found
kubectl delete service zookeeper --ignore-not-found
```

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
