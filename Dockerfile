FROM ubuntu:16.04

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-8-jdk \
    maven \
    && rm -rf /var/lib/apt/lists/*

COPY . /app
WORKDIR /app
