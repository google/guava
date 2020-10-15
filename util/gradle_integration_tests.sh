#!/bin/bash

set -eu

mvn clean install -DskipTests
mvn clean install -DskipTests -f android

integration-tests/gradle/gradlew -p integration-tests/gradle wrapper --gradle-version=5.6.4
integration-tests/gradle/gradlew -p integration-tests/gradle testClasspath
integration-tests/gradle/gradlew -p integration-tests/gradle wrapper --gradle-version=7.0.2
integration-tests/gradle/gradlew -p integration-tests/gradle testClasspath
