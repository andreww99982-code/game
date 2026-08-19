#!/bin/sh
#
# Gradle startup script for UN*X
#

# Attempt to set APP_HOME
SCRIPT="$0"
while [ -h "$SCRIPT" ] ; do
  ls=$(ls -ld "$SCRIPT")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '/.*' > /dev/null; then
    SCRIPT="$link"
  else
    SCRIPT="$(dirname "$SCRIPT")/$link"
  fi
done

APP_HOME="$(pwd -P)"

APP_NAME="Gradle"
APP_BASE_NAME="$(basename "$0")"

GRADLE_OPTS="${GRADLE_OPTS:-} -Xmx512m -Xms256m"

# Use java
if [ -z "$JAVA_HOME" ] ; then
  JAVA_CMD="java"
else
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVA_CMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
  "-Dorg.gradle.appname=$APP_BASE_NAME" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
