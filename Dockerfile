FROM maven:latest
LABEL authors="nealu"

WORKDIR /app
COPY pom.xml /app
COPY . /app
RUN mvn package
CMD ["java", "-jar", "target/my-project-1.0-SNAPSHOT.jar"]