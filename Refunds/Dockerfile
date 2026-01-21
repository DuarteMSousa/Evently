FROM eclipse-temurin:19-jdk-alpine

EXPOSE 8088

ADD target/evently_refunds.jar evently_refunds.jar

ENTRYPOINT ["java","-jar","evently_refunds.jar"]