#!/bin/bash
#
# Copyright (C) 2025-present OpenRefactory, Inc.
#
# SPDX-License-Identifier: Apache-2.0
#

usage()
{
cat << EOF
usage: $0 -i IMAGE_NAME

OPTIONS:
   -i IMAGE_NAME name of the docker image to build
EOF
exit 1
}

IMAGE_NAME="callgraph:latest"
while getopts ":hi:" OPTION
do
    case $OPTION in
        i) IMAGE_NAME=$OPTARG;;
        h|*) usage;;
    esac
done

if [ "$IMAGE_NAME" = "" ]; then
    usage
fi

# Build the `tar.gz` distribution
echo "==> Building the Callgraph distribution (.tar.gz)"
./mvnw clean package -DskipTests

# Build the docker image
echo "==> Building Docker image: $IMAGE_NAME"
docker build -t "$IMAGE_NAME" .

echo "Docker image $IMAGE_NAME built successfully."
