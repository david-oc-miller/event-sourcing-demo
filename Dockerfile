FROM eclipse-temurin:17-jdk-noble

WORKDIR /app

COPY target/event-sourcing-demo-*-fat.jar event-sourcing-demo.jar
COPY src/main/resources/wait-forever.sh wait-forever.sh

EXPOSE 8080

ENTRYPOINT ["bash", "wait-forever.sh"]
