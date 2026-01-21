FROM eclipse-temurin:19-jdk-alpine

EXPOSE 8084

ADD target/evently_notifications.jar evently_notifications.jar

ENTRYPOINT ["java","-jar","evently_notifications.jar"]