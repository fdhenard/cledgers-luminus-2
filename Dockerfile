FROM openjdk:8-alpine

COPY target/uberjar/cledgers-luminus.jar /cledgers-luminus/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/cledgers-luminus/app.jar"]
