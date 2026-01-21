FROM eclipse-temurin:19-jdk-alpine

EXPOSE 8080

ADD target/evently_api_gateway.jar evently_api_gateway.jar

ENTRYPOINT ["java","-jar","evently_api_gateway.jar"]