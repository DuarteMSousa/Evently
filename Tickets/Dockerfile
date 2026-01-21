FROM eclipse-temurin:19-jdk-alpine

EXPOSE 8091

ADD target/evently_tickets.jar evently_tickets.jar

ENTRYPOINT ["java","-jar","evently_tickets.jar"]