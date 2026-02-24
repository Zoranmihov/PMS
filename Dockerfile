FROM maven:3.9.12-eclipse-temurin-25-noble

WORKDIR /workspace

COPY app/pom.xml /workspace/pom.xml
RUN mvn -q -DskipTests dependency:go-offline

COPY app/ /workspace/

EXPOSE 8080

CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.profiles=docker"]