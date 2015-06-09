#!/bin/bash

set -e -u

if [ $# -eq 1 ]; then
  REF=$1
elif [ $# -eq 0 ]; then
  REF=master
else
  echo "Usage: updatejavadoc <branch-or-tag>" >&2
  exit 1
fi

if git diff --name-only | grep . -q ; then
  echo "Uncommitted changes found. Aborting." >&2
  exit 1
fi

# Make temp dir
DOCTEMP=$(mktemp -d -t guava-$REF-docs)

# Checkout the main code at the specified version
echo "Checking out $REF"
git checkout -q $REF

# Generate Javadoc and move it to temp dir
echo "Generating Javadoc"
mvn clean javadoc:javadoc -pl guava > /dev/null
mv guava/target/site/apidocs/* $DOCTEMP/
rm -fr guava/target

# Get the version name we want to use under the releases/ directory
if [[ $REF =~ v\d+.* ]]; then
  # For a version tag (e.g. v18.0), remove the v
  VERSION=${REF:1}
elif [ $REF == "master" ]; then
  # The master branch docs should be under "snapshot"
  VERSION=snapshot
else
  VERSION=$REF
fi

# Switch back to gh-pages branch
echo "Checking out gh-pages"
git checkout -q gh-pages

DOCSDIR=releases/$VERSION/api/docs

echo "Moving Javadoc to $DOCSDIR"
mkdir -p $DOCSDIR
rm -fr $DOCSDIR
mv $DOCTEMP $DOCSDIR

echo "Finished"
