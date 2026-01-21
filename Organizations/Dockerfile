FROM eclipse-temurin:19-jdk-alpine

EXPOSE 8086

ADD target/evently_organizations.jar evently_organizations.jar

ENTRYPOINT ["java","-jar","evently_organizations.jar"]