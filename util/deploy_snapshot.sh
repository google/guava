#!/bin/bash

# see https://coderwall.com/p/9b_lfq

set -e -u

function mvn_deploy() {
  ./mvnw clean deploy -DskipTests=true "$@"
}

echo "Publishing Maven snapshot..."

mvn_deploy
mvn_deploy -f android/pom.xml

echo "Maven snapshot published."
