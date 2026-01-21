FROM eclipse-temurin:19-jdk-alpine

EXPOSE 8095

ADD target/evently_events.jar evently_events.jar

ENTRYPOINT ["java","-jar","evently_events.jar"]