FROM eclipse-temurin:19-jdk-alpine

EXPOSE 8092

ADD target/evently_users.jar evently_users.jar

ENTRYPOINT ["java","-jar","evently_users.jar"]