FROM eclipse-temurin:19-jdk-alpine

EXPOSE 8083

ADD target/evently_file_generation.jar evently_file_generation.jar

ENTRYPOINT ["java","-jar","evently_file_generation.jar"]