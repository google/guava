#!/bin/bash

set -e -u

if [ $# -gt 1 ]; then
  echo "Usage: updatejavadoc <branch-or-tag>" >&2
  exit 1
fi

ref=$1

if [ -z $ref ]; then
  ref=master
fi

# cd to git root dir (the dir above the one containing this script):
initialdir=$pwd
cd $(dirname $0)
cd ..

if git diff --name-only | grep . -q ; then
  echo "Uncommitted changes found. Aborting." >&2
  exit 1
fi

# Make temp dir
doctemp=$(mktemp -d -t guava-$ref-docs)

# Checkout the main code at the specified version
echo "Checking out $ref"
git checkout -q $ref

# Generate Javadoc and move it to temp dir
echo "Generating Javadoc"

# TODO(cgdecker): Redirect output to a temp log file and tell user about it if something goes wrong
mvn clean javadoc:javadoc -pl guava > /dev/null
mv guava/target/site/apidocs/* $doctemp/
rm -fr guava/target

# Get the version name we want to use under the releases/ directory
if [[ $ref =~ v\d+.* ]]; then
  # For a version tag (e.g. v18.0), remove the v
  version=${REF:1}
elif [ $ref == "master" ]; then
  # The master branch docs should be under "snapshot"
  version=snapshot
else
  version=$ref
fi

# Switch back to gh-pages branch
echo "Checking out gh-pages"
git checkout -q gh-pages

docsdir=releases/$version/api/docs

echo "Moving Javadoc to $docsdir"
mkdir -p $docsdir
rm -fr $docsdir
mv $doctemp $docsdir

cd $initialdir

echo "Finished"
