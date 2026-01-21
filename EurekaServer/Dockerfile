FROM eclipse-temurin:19-jdk-alpine

EXPOSE 8761

ADD target/evently_eureka_server.jar evently_eureka_server.jar

ENTRYPOINT ["java","-jar","evently_eureka_server.jar"]