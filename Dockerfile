FROM openjdk:11-jdk-slim-buster as builder

RUN mkdir /workspace

WORKDIR /workspace

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew build || return 0

COPY . /workspace

RUN ./gradlew example:installDist --no-daemon

FROM openjdk:11-jdk-slim-buster as runner
WORKDIR /home
COPY --from=builder /workspace/example/build/install .

ENTRYPOINT ["/home/example/bin/example"]

EXPOSE 8080
