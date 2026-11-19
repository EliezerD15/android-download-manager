#!/bin/sh

# Gradle wrapper launcher for POSIX systems.
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "${JAVA_HOME:-}" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
    if [ ! -x "$JAVACMD" ]; then
        echo "ERROR: JAVA_HOME no apunta a una instalación válida de Java." >&2
        exit 1
    fi
else
    JAVACMD=java
    if ! command -v "$JAVACMD" >/dev/null 2>&1; then
        echo "ERROR: No se encontró Java. Instala JDK 17 y configura JAVA_HOME." >&2
        exit 1
    fi
fi

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "ERROR: No se encuentra gradle/wrapper/gradle-wrapper.jar." >&2
    exit 1
fi

exec "$JAVACMD" ${JAVA_OPTS:-} ${GRADLE_OPTS:-} \
    -Dorg.gradle.appname=gradlew \
    -classpath "$WRAPPER_JAR" \
    org.gradle.wrapper.GradleWrapperMain "$@"
