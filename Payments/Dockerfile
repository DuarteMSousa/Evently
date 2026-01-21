FROM eclipse-temurin:19-jdk-alpine

EXPOSE 8087

ADD target/evently_payments.jar evently_payments.jar

ENTRYPOINT ["java","-jar","evently_payments.jar"]