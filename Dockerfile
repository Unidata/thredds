FROM ubuntu:20.04

ARG BUILDER_UID=9999
ARG DEBIAN_FRONTEND=noninteractive

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64
ENV JAVA_TOOL_OPTIONS -Duser.home=/home/builder

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    git \
    openjdk-8-jdk \
    python3-dev \
    maven \
    wget \
    && rm -rf /var/lib/apt/lists/*

RUN update-alternatives --install /usr/bin/python python /usr/bin/python3 10

RUN wget -q https://bootstrap.pypa.io/get-pip.py \
    && python get-pip.py pip==22.0.3 setuptools==60.8.1 wheel==0.37.1 \
    && rm -rf get-pip.py

RUN pip install \
    bump2version==0.5.10

RUN useradd --create-home --no-log-init --shell /bin/bash --uid $BUILDER_UID builder
USER builder
WORKDIR /home/builder
