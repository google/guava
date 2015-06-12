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

cd $(dirname $0)/..
source _bin/util.sh

ensure_no_uncommitted_changes

if [[ $# -eq 0 ]]; then
  version="snapshot"
elif [[ $# -eq 1 ]]; then
  version=$(parse_version $1)
else
  echo "Usage: $0 <version>" >&2
  exit 1
fi

# Switch to the git ref for the target version
ghpagesref=$(currentref)
checkout $(git_ref $version)

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

# Get the Guava version
guavaversion=$(guava_version)

# Switch back to gh-pages
echo "Checking out '$ghpagesref'"
git checkout -q $ghpagesref

outputdir=releases/$version/api/diffs/

# Run JDiff
echo "Running JDiff"
javadoc \
  -sourcepath $srctemp \
  -classpath $classpath \
  -subpackages com.google.common \
  -encoding UTF-8 \
  -doclet jdiff.JDiff \
  -docletpath _lib/jdiff.jar:_lib/xerces-for-jdiff.jar \
  -apiname "Guava $guavaversion" \
  -apidir $outputdir \
  -exclude com.google.common.base.internal \
  -protected

# Rename the output file
outputfile=$outputdir/Guava_$guavaversion.xml
echo "Renaming 'Guava_$guavaversion.xml' to '$version.xml'"
mv $outputfile $outputdir/$version.xml

# Cleanup temp files
echo "Cleaning up temp files"
rm -fr $srctemp
rm -fr $classtemp

# Commit
echo "Committing changes"
git add .
git commit -m "Generate JDiff XML file for version $guavaversion"

echo "Finished"
