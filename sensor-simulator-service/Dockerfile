# Use an official OpenJDK 21 runtime as a parent image
FROM openjdk:21-slim

# Set the working directory in the container
WORKDIR /app

# Install SBT and Scala
RUN apt-get update && \
    apt-get install -y curl gnupg && \
    echo "deb [https://repo.scala-sbt.org/scalasbt/debian](https://repo.scala-sbt.org/scalasbt/debian) all main" | tee /etc/apt/sources.list.d/sbt.list && \
    echo "deb [https://repo.scala-sbt.org/scalasbt/debian](https://repo.scala-sbt.org/scalasbt/debian) /" | tee /etc/apt/sources.list.d/sbt_old.list && \
    curl -sL "[https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823"](https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823") | apt-key add && \
    apt-get update && \
    apt-get install -y sbt scala && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy project files
COPY . /app

# Compile and create fat JAR
RUN sbt clean assembly

# Expose any necessary ports (if needed)
# EXPOSE 8080

# Run the application
CMD ["java", "-jar", "target/scala-2.12/sensor-simulator-service.jar"]