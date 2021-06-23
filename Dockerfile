FROM openjdk:8-alpine

COPY target/uberjar/cledgers.jar /cledgers/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/cledgers/app.jar"]
