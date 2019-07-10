#!/bin/bash

# This script compiles Guava, performing type-checking.

# If optional argument $1 is "-P checkerframework-local",
# use a locally-built clone of the Checker Framework repository.
export MVN_FLAGS=$1

# Fail the whole script if any command fails.
set -e

## Diagnostic output
# Output lines of this script as they are read.
set -o verbose
# Output expanded lines of this script as they are executed.
set -o xtrace

export SHELLOPTS

git -C /tmp/plume-scripts pull > /dev/null 2>&1 \
  || git -C /tmp clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git
SLUGOWNER=`/tmp/plume-scripts/git-organization typetools`

echo TRAVIS_PULL_REQUEST_BRANCH = $TRAVIS_PULL_REQUEST_BRANCH
echo TRAVIS_BRANCH = $TRAVIS_BRANCH

## Build Checker Framework
REPO=`/tmp/plume-scripts/git-find-fork ${SLUGOWNER} typetools checker-framework`
BRANCH=`/tmp/plume-scripts/git-find-branch ${REPO} ${TRAVIS_PULL_REQUEST_BRANCH:-$TRAVIS_BRANCH}`
(cd .. && git clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO} checker-framework) || (cd .. && git clone -b ${BRANCH} --single-branch --depth 1 -q ${REPO} checker-framework)
# This also builds annotation-tools and jsr308-langtools
(cd ../checker-framework/ && ./.travis-build-without-test.sh downloadjdk)
export CHECKERFRAMEWORK=`readlink -f ../checker-framework`

(cd guava && mvn package $MVN_FLAGS -Dmaven.test.skip=true -Danimal.sniffer.skip=true)
