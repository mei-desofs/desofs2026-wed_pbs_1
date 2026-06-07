FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

RUN useradd --system --create-home --home-dir /opt/ghostreport ghostreport
WORKDIR /opt/ghostreport

COPY --from=build /workspace/target/ghostreport-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p uploads backups \
    && chown -R ghostreport:ghostreport /opt/ghostreport

USER ghostreport
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD ["bash", "-ec", "exec 3<>/dev/tcp/127.0.0.1/8081; printf 'GET /index.html HTTP/1.1\\r\\nHost: localhost\\r\\nConnection: close\\r\\n\\r\\n' >&3; grep -q '200' <&3"]

ENTRYPOINT ["java", "-jar", "app.jar"]
