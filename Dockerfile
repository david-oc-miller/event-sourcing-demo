FROM eclipse-temurin:25-jdk-noble

WORKDIR /app

COPY event-sourcing-library/target/event-sourcing-library-*.jar .
COPY event-sourcing-library/src/main/resources/wait-forever.sh wait-forever.sh

EXPOSE 8080

ENTRYPOINT ["bash", "wait-forever.sh"]
