#!/bin/bash

# Script to generate Antlr parser files from Type.g4 grammar
# and copy them to the appropriate source directory

JAVA_CALLGRAPH_PATH="/home/openrefactory/OpenRefactory/java-callgraph/bundles/org.openrefactory.callgraph"

java -jar ../lib/antlr-4.9.1-complete.jar -package org.openrefactory.analysis.antlr -visitor Type.g4

rm ${JAVA_CALLGRAPH_PATH}/src/org/openrefactory/analysis/antlr/*.java
cp *.java ${JAVA_CALLGRAPH_PATH}/src/org/openrefactory/analysis/antlr/

rm *.java
rm *.interp
rm *.tokens
