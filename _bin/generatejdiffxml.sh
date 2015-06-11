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
  dir=$version
fi
outputdir=releases/$dir/api/diffs/

# Switch to the git ref for the target version
ghpagesref=$(_bin/currentref.sh)
echo "Checking out '$ref'"
git checkout -q $ref

# If we're on master, pull to get the latest
if [ $ref == "master" ]; then
  echo "Pulling to get latests changes"
  git pull -q
fi

# Copy the Guava source code to a temp dir
srctemp=$(mktemp -d -t guava-$version-src.XXX)
echo "Copying source files to '$srctemp'"
cp -r guava/src/* $srctemp/

# Get a classpath for Guava's dependencies and compiled code
deptemp=$(mktemp -t guava-$version-deps.XXX)
echo "Compiling Guava"
# TODO(cgdecker): Output to a log file and tell the user about it if something goes wrong
mvn clean compile dependency:build-classpath -Dmdep.outputFile=$deptemp -pl guava > /dev/null
classpath=$(cat $deptemp)
rm $deptemp
classtemp=$(mktemp -d -t guava-$version-classes.XXX)
echo "Copying class files to '$classtemp'"
cp -r guava/target/classes/* $classtemp/
classpath=$classtemp:$classpath

# Switch back to gh-pages
echo "Checking out '$ghpagesref'"
git checkout -q $ghpagesref

# Run JDiff
echo "Running JDiff"
javadoc \
  -sourcepath $srctemp \
  -classpath $classpath \
  -subpackages com.google.common \
  -encoding UTF-8 \
  -doclet jdiff.JDiff \
  -docletpath _lib/jdiff.jar:_lib/xerces-for-jdiff.jar \
  -apiname "Guava $version" \
  -apidir $outputdir \
  -exclude com.google.common.base.internal \
  -protected

# Rename the output file
outputfile=$outputdir/Guava_$version.xml
echo "Renaming 'Guava_$version.xml' to '$dir.xml'"
mv $outputfile $outputdir/$dir.xml

# Cleanup temp files
echo "Cleaning up temp files"
rm -fr $srctemp
rm -fr $classtemp

# Commit
echo "Committing changes"
git add .
git commit -m "Generate jdiff xml file for $version"

echo "Finished"
