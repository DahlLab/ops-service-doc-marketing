FROM openjdk:25-ea
EXPOSE 8080
ADD backend/target/app.jar app-in-image.jar
ENTRYPOINT ["java", "-jar", "app-in-image.jar"]