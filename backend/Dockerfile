FROM gradle:8.13-jdk17 AS build
WORKDIR /project
COPY . .
RUN gradle --no-daemon installDist --console=plain

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S onechat && adduser -S -G onechat onechat
COPY --from=build /project/build/install/onechat-preview-backend/ /app/
USER onechat
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0 -XX:+UseSerialGC"
EXPOSE 8080
CMD ["/app/bin/onechat-preview-backend"]
