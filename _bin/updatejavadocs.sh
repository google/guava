#!/bin/bash

set -e -u

#*****************************************************************************
#
# This script generates/updates the Javadoc for a specific
# version of Guava, committing the Javadoc to the api/docs/
# directory of that version under releases/.
#
# This can be used in a couple different ways:
#
#   # Builds Javadoc for the 'master' branch, commits to releases/snapshot
#   _bin/updatejavadoc.sh
#   _bin/updatejavadoc.sh master
#   _bin/updatejavadoc.sh snapshot
#   _bin/updatejavadoc.sh 19.0-SNAPSHOT
#
#   # Builds Javadoc for the 'v18.0' tag, commits to releases/18.0
#   _bin/updatejavadoc.sh 18.0
#
#******************************************************************************

if [ $# -eq 0 ]; then
  version=snapshot
elif [ $# -eq 1 ]; then
  version=$1
else
  echo "Usage: updatejavadoc.sh <version>" >&2
  exit 1
fi

if [ -z $version ]; then
  version=snapshot
  ref=master
elif [ $version == master ]; then
  version=snapshot
  ref=master
elif [ $version == snapshot ]; then
  ref=master
elif [[ $version =~ [0-9]+\..+ ]]; then
  # The version starts with numbers and a dot (a release version)
  if [[ $version =~ .+-SNAPSHOT ]]; then
    # If we get any -SNAPSHOT version, use master
    version=snapshot
    ref=master
  else
    # If the version isn't 'master', prepend v to it to get the tag to check out
    ref=v$version
  fi
else
  echo "Invalid version specified: $version"
  exit 1
fi

# cd to git root dir (the dir above the one containing this script):
initialdir=$PWD
cd $(dirname $0)
cd ..

if git diff --name-only | grep . -q ; then
  echo "Uncommitted changes found. Aborting." >&2
  exit 1
fi

# Make temp dir
doctemp=$(mktemp -d -t guava-$version-docs)

# Checkout the main code at the specified version
echo "Checking out $ref"
git checkout -q $ref

# Generate Javadoc and move it to temp dir
echo "Generating Javadoc"

# TODO(cgdecker): Redirect output to a temp log file and tell user about it if something goes wrong
mvn clean javadoc:javadoc -pl guava > /dev/null
mv guava/target/site/apidocs/* $doctemp/
rm -fr guava/target

# Switch back to gh-pages branch
echo "Checking out gh-pages"
git checkout -q gh-pages

docsdir=releases/$version/api/docs

echo "Moving Javadoc to $docsdir"
mkdir -p $docsdir
rm -fr $docsdir
mv $doctemp $docsdir

echo "Committing changes"
git add .
git commit -m "Generate Javadoc for version $version"

cd $initialdir

echo "Finished"
