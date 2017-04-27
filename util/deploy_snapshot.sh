#!/bin/bash

# see https://coderwall.com/p/9b_lfq

set -e -u

if [ "$TRAVIS_REPO_SLUG" == "google/guava" ] && \
   [ "$TRAVIS_JDK_VERSION" == "oraclejdk8" ] && \
   [ "$TRAVIS_PULL_REQUEST" == "false" ] && \
   [ "$TRAVIS_BRANCH" == "master" ]; then
  echo "Publishing Maven snapshot..."

  mvn_command=(
    mvn clean source:jar javadoc:jar deploy --settings="$(dirname $0)/settings.xml" -DskipTests=true
  )
  "${mvn_command[@]}"

  mvn_command+=(-f android/pom.xml
  "${mvn_command[@]}"              

  echo "Maven snapshot published."
fi
