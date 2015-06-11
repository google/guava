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

# defaults
version=snapshot
ref=master

if [ $# -eq 1 ]; then
  if [[ ! $1 =~ .+-SNAPSHOT ]]; then
    # If we didn't get a -SNAPSHOT version (in which case we should just use master)
    if [[ $1 =~ [0-9]+\..+ ]]; then
      # The version starts with numbers and a dot (a release version)
      version=$1
      ref=v$version
    else
      echo "Invalid version specified: $version" >&2
      exit 1
    fi
  fi
elif [ $# -gt 1 ]; then
  echo "Usage: $0 [<version>]" >&2
  exit 1
fi

# cd to git root dir (the dir above the one containing this script):
cd $(dirname $0)
cd ..

if git diff --name-only | grep . -q ; then
  echo "Uncommitted changes found. Aborting." >&2
  exit 1
fi

# Make temp dir
doctemp=$(mktemp -d -t guava-$version-docs.XXX)

# Checkout the main code at the specified version
echo "Checking out '$ref'"
git checkout -q $ref

# Generate Javadoc and move it to temp dir
echo "Generating Javadoc"

# TODO(cgdecker): Redirect output to a temp log file and tell user about it if something goes wrong
mvn clean javadoc:javadoc -pl guava > /dev/null
echo "Moving Javadoc to '$doctemp'"
mv guava/target/site/apidocs/* $doctemp/
rm -fr guava/target

# Switch back to gh-pages branch
echo "Checking out 'gh-pages'"
git checkout -q gh-pages

docsdir=releases/$version/api/docs

echo "Moving Javadoc to '$docsdir'"
mkdir -p $docsdir
rm -fr $docsdir
mv $doctemp $docsdir

echo "Committing changes"
git add .
git commit -m "Generate Javadoc for version $version"

echo "Finished"
