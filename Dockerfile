# Base-image: OpenjDK 21
FROM eclipse-temurin:21-jdk

# Install:
# - Xvfb: to run the GUI application in a virtual framebuffer
# - libgtk-3-0: GTK+ 3 library for graphical applications
# - Clean up apt cache to reduce image size
RUN apt -y update && \
    apt install -y xvfb libgtk-3-0 && \
    # Clean up
    apt clean all && \
    rm -rf /var/lib/apt/lists/*

# Unpack the binary distribution at the desired location
ADD products/target/products/org.openrefactory.callgraph.product-linux.gtk.x86_64.tar.gz /usr/local/callgraph/

# Prepare the workspace
RUN mkdir -p /workspace && \
    echo '{"source":"/workspace/source","result":"/workspace/result","summaries":"/usr/local/callgraph/summaries"}' > /workspace/config.json && \
    mkdir -p /workspace/source && \
    mkdir -p /workspace/result
VOLUME ["/workspace/source", "/workspace/result"]

# Copy the entrypoint script
ADD Docker/docker-entrypoint.sh /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
