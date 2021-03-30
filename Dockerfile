FROM gradle:jdk11 as builder

RUN mkdir /workspace

WORKDIR /workspace

COPY . /workspace

RUN gradle test example:installDist --no-daemon

FROM openjdk:11-jdk-slim-buster as runner
WORKDIR /home
COPY --from=builder /workspace/example/build/install .

ENTRYPOINT ["/home/example/bin/example"]

EXPOSE 8080
