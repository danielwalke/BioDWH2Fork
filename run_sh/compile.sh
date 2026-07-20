#!/usr/bin/env bash

# Compiles locally and transfers Neo4j.jar and BioDWH2.jar to workstation
set -euo pipefail


BIO_DWH2_HOME="/home/benjamin.reyes/BioDWH2/BioDWH2Fork"
POM_FILE="$BIO_DWH2_HOME/src/pom.xml"

NEW_VERSION="${1:-0.68_KG1_V2}"

echo "Using BioDWH2 version: $NEW_VERSION"

mvn -f "$POM_FILE" versions:set -DnewVersion="$NEW_VERSION"
mvn -f "$POM_FILE" clean package -DskipTests

echo "Ready to run on workstation."
