#!/usr/bin/env bash
# Builds both jars from source (alternative to restore_jars.sh).
#  1. BioDWH2 jar from this repository
#  2. BioDWH2-Neo4j-Server from its upstream repository, rebuilt against this fork's core (0.6.8d) because the
#     stock release bundles core 0.5.1 and cannot read graphs written by this fork.
# Requires: JDK 17+, Maven, git
set -euo pipefail
repo="$(cd "$(dirname "$0")/.." && pwd)"
core_version="0.6.8d"
out="$repo/dist/out"
mkdir -p "$out"

mvn -q -f "$repo/src/pom.xml" clean package -DskipTests
cp "$repo/src/biodwh2-main/target/BioDWH2-v${core_version}.jar" "$out/BioDWH2-v${core_version}.jar"

# install the fork's core so the server build resolves ${core_version} from the local Maven repository
mvn -q -f "$repo/src/pom.xml" install -DskipTests -pl biodwh2-core -am

server_dir="$(mktemp -d)"
trap 'rm -rf "$server_dir"' EXIT
git clone -q https://github.com/BioDWH2/BioDWH2-Neo4j-Server.git "$server_dir/server"
sed -i "s#<version>0.5.1</version>#<version>${core_version}</version>#" "$server_dir/server/src/pom.xml"
mvn -q -f "$server_dir/server/src/pom.xml" clean package -DskipTests
cp "$server_dir/server/src/target/BioDWH2-Neo4j-Server-v1.3.2.jar" \
   "$out/BioDWH2-Neo4j-Server-v1.3.2-core${core_version}.jar"
ls -la "$out"
