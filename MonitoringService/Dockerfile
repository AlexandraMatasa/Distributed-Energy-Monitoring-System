FROM maven:3.9.9-eclipse-temurin-21 AS build

ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENV CATALINA_OPTS="-Duser.timezone=UTC"

COPY ./pom.xml /app/pom.xml
WORKDIR /app
RUN mvn dependency:go-offline
COPY ./src/ /app/src
WORKDIR /app
RUN mvn clean install -DskipTests

FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/MonitoringService-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8084
CMD ["java", "-jar", "app.jar"]
