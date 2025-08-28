# Base-image: Ubuntu 22.04
FROM ubuntu:jammy

# Update and install required packages.
# Required packages:
#   - Java 17 (openjdk-17-jdk)
#   - Xvfb

WORKDIR /workspace

RUN apt -y update && \
    apt install -y --no-install-recommends gcc xvfb openjdk-17-jre-headless libswt-gtk-4-jni libswt-gtk-4-java && \
    # Clean up
    apt clean all && \
    rm -rf /var/lib/apt/lists/*

COPY . .

RUN chmod -R 777 /usr/local/ && \
    chmod +x /workspace/configure_run.sh && \
    echo '{\n\
    "source": "/workspace/source",\n\
    "result": "/workspace/result",\n\
    "summaries": "/workspace/summaries"\n\
}' > config.json

ENTRYPOINT ["/workspace/configure_run.sh"]
