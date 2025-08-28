#!/bin/bash

# xvfb-run is a wrapper for the Xvfb(1x) command which simplifies the task of running commands (typically an X client, or a script containing a list of clients to be run) within a virtual X server environment.
xvfb-run \
java \
-Xmx8192m \
-Xss4m -XX:-UseTLAB -XX:+UseG1GC -XX:-UseTransparentHugePages -XX:+AlwaysPreTouch -XX:-OmitStackTraceInFastThrow \
-classpath /workspace/eclipse-plugins/org.eclipse.equinox.launcher_1.6.600.v20231106-1826.jar \
org.eclipse.equinox.launcher.Main \
-application org.openrefactory.callgraph.id1 \
-configuration file:/workspace/ \
-data /workspace/runtime-org.openrefactory.callgraph.id1
