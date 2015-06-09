#!/bin/bash

#********************************************************
#
# This script is for generating the JDiff XML file for a
# particular version. Usage:
#
#   _bin/generatejdiffxml.sh 18.0
#
# Using any -SNAPSHOT version as the new version will
# cause the file to be output to:
#
#   /releases/snapshot/api/diffs/snapshot.xml
#
# Otherwise, the output file will be located at:
#
#   /releases/$version/api/diffs/$version.xml
#
#********************************************************

set -e -u

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <version>" >&2
  exit 1
fi

version=$1

# cd to git root dir (the dir above the one containing this script):
cd $(dirname $0)
cd ..

if git diff --name-only | grep . -q ; then
  echo "Uncommitted changes found. Aborting." >&2
  exit 1
fi

# Output to /releases/snapshot if the new version is a snapshot
if [[ $version =~ .+-SNAPSHOT ]]; then
  # snapshots come from master
  ref=master
  dir=snapshot
else
  # releases come from a tag of the form "v18.0"
  ref=v$version
  dir=$new
fi
outputdir=releases/$dir/api/diffs/

# Switch to the git ref for the target version
echo "Checking out $ref"
git checkout -q $ref

# Copy the Guava source code to a temp dir
srctemp=$(mktemp -d -t guava-$version-jdiff)
echo "Copying source files to $srctemp"
cp -r guava/src/* $srctemp/

# Copy the Guava dependencies to a temp dir
deptemp=$(mktemp -t guava-$version-deps)
# TODO(cgdecker): Output to a log file and tell the user about it if something goes wrong
mvn dependency:build-classpath -Dmdep.outputFile=$deptemp -pl guava > /dev/null
classpath=$(cat $deptemp)
rm $deptemp

# Switch back to gh-pages
echo "Checking out gh-pages"
git checkout -q gh-pages

# Run JDiff
echo "Running JDiff"
javadoc \
  -sourcepath $srctemp \
  -classpath $classpath \
  -subpackages com \
  -encoding UTF-8 \
  -doclet jdiff.JDiff \
  -docletpath _lib/jdiff.jar:_lib/xerces-for-jdiff.jar \
  -apiname "Guava $version" \
  -apidir $outputdir

# Rename the output file if this was for a snapshot
if [ $dir == "snapshot" ]; then
  outputfile=$outputdir/Guava_$version.xml
  mv $outputfile $outputdir/snapshot.xml
fi

rm -fr $srctemp

echo "Finished"
