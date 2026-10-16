#!/usr/bin/env bash
# Restores the runnable jars from the compressed files in this directory (GitHub rejects files > 100 MB).
# Output: ./out/BioDWH2-v0.6.8d.jar and ./out/BioDWH2-Neo4j-Server-v1.3.2-core0.6.8d.jar
# Requires: xz, tar, zip
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
xz -dc BioDWH2-v0.6.8d.jar.xz > out/BioDWH2-v0.6.8d.jar

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
xz -dc BioDWH2-Neo4j-Server-v1.3.2-core0.6.8d.tar.xz | tar -xf - -C "$tmp"
jar_out="$PWD/out/BioDWH2-Neo4j-Server-v1.3.2-core0.6.8d.jar"
rm -f "$jar_out"
(cd "$tmp" && zip -q -X -0 "$jar_out" META-INF/MANIFEST.MF && zip -q -X -r -9 "$jar_out" . -x META-INF/MANIFEST.MF)
ls -la out
