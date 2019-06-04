# Multi-stage build setup (https://docs.docker.com/develop/develop-images/multistage-build/)

# Stage 1 (to create a "build" image)
FROM openjdk:8-jdk-alpine AS builder
RUN java -version

COPY . /usr/src/myapp/
WORKDIR /usr/src/myapp/
RUN apk --no-cache add maven && mvn --version
RUN mvn package

# Stage 2 (to create a downsized "container executable")
FROM openjdk:8-jdk-alpine
WORKDIR /root/
COPY --from=builder /usr/src/myapp/target/api-gateway-1.0-SNAPSHOT.jar .

EXPOSE 8080
CMD java -Xmx256m -jar ./api-gateway-1.0-SNAPSHOT.jar --server.port=$PORT
