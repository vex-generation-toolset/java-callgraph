#!/bin/bash

#--------------------------------------------------------------------------------------------
# Script to download eclipse plugins and build docker image.
#
# Instructions:
#       Before using this script, the jar should be built using mvn command.
#       If the eclipse-plugins is added, will use that instead of downloading again.
#
#       usage:  sudo ./build_image.sh -i IMAGE_NAME
#       OPTIONS:
#              -i IMAGE_NAME name of the docker image to build
#
# --------------------------------------------------------------------------------------------

usage()
{
cat << EOF
usage: $0 -i IMAGE_NAME

OPTIONS:
   -i IMAGE_NAME name of the docker image to build
EOF
exit 1
}

IMAGE_NAME=""
while getopts ":oi:" OPTION
do
    case $OPTION in
        i) IMAGE_NAME=$OPTARG;;
        \?) usage;;
    esac
done

if [ "$IMAGE_NAME" = "" ]; then
    usage
fi

# Copy jar and summaries
cp ../target/org.openrefactory.callgraph-1.0.0-SNAPSHOT.jar . &&
cp -r ../summaries/ .

# Download eclipse and keep plugins in the eclipse-plugins directory
if [ ! -d eclipse-plugins ]; then
    echo "eclipse plugins are not found, downloading..." && \
    wget https://archive.eclipse.org/technology/epp/downloads/release/2023-12/R/eclipse-committers-2023-12-R-linux-gtk-x86_64.tar.gz && \
    tar xzf eclipse-committers-2023-12-R-linux-gtk-x86_64.tar.gz eclipse && \
    mv eclipse/plugins eclipse-plugins && \
    rm -rf eclipse eclipse-committers-2023-12-R-linux-gtk-x86_64.tar.gz
fi

# Build the docker image
echo "Building docker image ${IMAGE_NAME}" 
docker build -f Dockerfile -t ${IMAGE_NAME} .

# Remove jar and summaries after docker build
rm -rf org.openrefactory.callgraph-1.0.0-SNAPSHOT.jar summaries
