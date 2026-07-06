#!/bin/bash

set -eu

./mvnw \
  --projects '!guava-testlib,!guava-tests,!guava-bom,!guava-gwt' \
  -Dmaven.test.skip=true \
  -Dmaven.javadoc.skip=true \
  -ntp \
  clean install
./mvnw \
  -f android \
  --projects '!guava-testlib,!guava-tests,!guava-bom' \
  -Dmaven.test.skip=true \
  -Dmaven.javadoc.skip=true \
  -ntp \
  clean install

# We run these separately so that their changes to the default toolchain doesn't affect anything else.
# (And we run them after the main build so that that build has already downloaded Java 11/17 if necessary.)

./mvnw \
  --projects '!guava-testlib,!guava-tests,!guava-bom,!guava-gwt' \
  -ntp \
  initialize -P print-java-11-home
JAVA_11_HOME=$(<target/java_11_home)

./mvnw \
  --projects '!guava-testlib,!guava-tests,!guava-bom,!guava-gwt' \
  -ntp \
  initialize -P print-java-17-home
JAVA_17_HOME=$(<target/java_17_home)

# Gradle Wrapper overwrites some files when it runs.
# To avoid modifying the Git client, we copy everything we need to another directory.
# That provides general hygiene, including avoiding release errors:
#
# Preparing to update Javadoc and JDiff for the release...
# error: Your local changes to the following files would be overwritten by checkout:
#         integration-tests/gradle/gradle/wrapper/gradle-wrapper.jar
#         integration-tests/gradle/gradle/wrapper/gradle-wrapper.properties
#         integration-tests/gradle/gradlew
#         integration-tests/gradle/gradlew.bat
# Please commit your changes or stash them before you switch branches.

GRADLE_TEMP="$(mktemp -d)"
trap 'rm -rf "${GRADLE_TEMP}"' EXIT

# The Gradle tests need the pom.xml only to read its version number.
# (And the file needs to be two directory levels up from the Gradle build file.)
# TODO(cpovirk): Find a better way to give them that information.
cp pom.xml "${GRADLE_TEMP}"

for version in 5.6.4 7.0.2; do
  # Enter a subshell so that we return to the current directory afterward.
  (
    cp -r integration-tests "${GRADLE_TEMP}/${version}"
    cd "${GRADLE_TEMP}/${version}/gradle"
    JAVA_HOME="${JAVA_17_HOME}" ./gradlew wrapper --gradle-version="${version}"
    JAVA_HOME="${JAVA_11_HOME}" ./gradlew testClasspath
  )
done
