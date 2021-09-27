FROM amazoncorretto:17 as builder

RUN mkdir /workspace

WORKDIR /workspace

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew build || return 0

COPY . /workspace

RUN ./gradlew installDist --no-daemon

FROM amazoncorretto:17 as runner
WORKDIR /home
COPY --from=builder /workspace/build/install .

ENV JAVA_OPTS=-XX:+UseZGC
ENTRYPOINT ["/home/example/bin/example"]

EXPOSE 8080
