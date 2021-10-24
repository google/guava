#!/bin/bash

set -e -u

echo "Publishing Javadoc and JDiff..."

cd $HOME
git clone -q -b gh-pages "https://x-access-token:${GITHUB_TOKEN}@github.com/google/guava.git" gh-pages > /dev/null
cd gh-pages

git config --global user.name "$GITHUB_ACTOR"
git config --global user.email "$GITHUB_ACTOR@users.noreply.github.com"

./updaterelease.sh snapshot

git push -fq origin gh-pages > /dev/null

echo "Javadoc and JDiff published to gh-pages."
