#!/bin/bash

# see https://coderwall.com/p/9b_lfq

set -e -u

function mvn_deploy() {
  mvn clean source:jar javadoc:jar deploy \
    --settings="$(dirname $0)/settings.xml" -DskipTests=true "$@"
}

if [ "$TRAVIS_REPO_SLUG" == "google/guava" ] && \
   [ "$TRAVIS_JDK_VERSION" == "oraclejdk8" ] && \
   [ "$TRAVIS_PULL_REQUEST" == "false" ] && \
   [ "$TRAVIS_BRANCH" == "master" ]; then
  echo "Publishing Maven snapshot..."

  mvn_deploy
  mvn_deploy -f android/pom.xml

  echo "Maven snapshot published."
fi
