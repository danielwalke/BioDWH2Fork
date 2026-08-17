#!/usr/bin/env bash

# Compiles locally and transfers Neo4j.jar and BioDWH2.jar to workstation
set -euo pipefail


BIO_DWH2_HOME="/home/benjamin.reyes/BioDWH2/BioDWH2Fork"
POM_FILE="$BIO_DWH2_HOME/src/pom.xml"
NEW_VERSION="${1:-0.68_Version1_KG_noTrEMBL}"
JAR_MAIN="$BIO_DWH2_HOME/src/biodwh2-main/target/BioDWH2-v${NEW_VERSION}.jar"



echo "Using BioDWH2 version: $NEW_VERSION"

mvn -f "$POM_FILE" versions:set -DnewVersion="$NEW_VERSION"
mvn -f "$POM_FILE" clean package -DskipTests

if 
[[ ! -f "$JAR_MAIN" ]]; then
    echo "ERROR: BioDWH2 jar not found:"
    echo "  $JAR_MAIN"
    exit 1
fi

cp "$JAR_MAIN" "/home/benjamin.reyes/BioDWH2/BioDWH2_compile_versions/"

echo "Ready to paste to /home/benjamin.reyes/BioDWH2/BioDWH2_compile_versions."
