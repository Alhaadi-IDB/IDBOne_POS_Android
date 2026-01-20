#!/usr/bin/env sh

set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "gradle-wrapper.jar is missing. Please run 'gradle wrapper' once or let Android Studio generate it." >&2
  exit 1
fi

exec java -jar "$WRAPPER_JAR" "$@"
