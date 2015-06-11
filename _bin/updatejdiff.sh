#!/bin/bash

#********************************************************
#
# This script is only intended to be used for updating
# the API diffs for the Guava site, not for generating
# arbitrary diffs. Except when doing releases, it should
# generally be invoked as:
#
#   _bin/updatejdiff.sh 18.0 19.0-SNAPSHOT
#
# Using any -SNAPSHOT version as the new version will
# case the diffs to go in /releases/snapshot.
#
# For releases, just use a non-snapshot version instead:
#
#   _bin/updatejdiff.sh 18.0 19.0
#
# This will put the diffs in /releases/19.0
#
#********************************************************

set -e -u

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <previous version> <version>" >&2
  exit 1
fi

old=$1
new=$2

# cd to git root dir (the dir above the one containing this script):
cd $(dirname $0)
cd ..

if git diff --name-only | grep . -q ; then
  echo "Uncommitted changes found. Aborting." >&2
  exit 1
fi

# Output to /releases/snapshot if the new version is a snapshot
if [[ $new =~ .+-SNAPSHOT ]]; then
  newdir=snapshot
else
  newdir=$new
fi
outputdir=releases/$newdir/api/diffs/

# These are the base paths to Javadoc that will be used in the
# generated changes html files. Use paths relative to the directory
# where those files will go (/releases/$newdir/api/diffs/changes).

# /releases/$new/api/docs/
newjavadoc=../../docs/
# /releases/$old/api/docs/
oldjavadoc=../../../../$old/api/docs/

# Copy JDiff xml files to current dir with the names JDiff expects
tempoldxml=Guava_$old.xml
cp releases/$old/api/diffs/$old.xml $tempoldxml
tempnewxml=Guava_$new.xml
cp releases/$newdir/api/diffs/$newdir.xml $tempnewxml

# Run JDiff
echo "Running JDiff"
javadoc \
  -subpackages com \
  -doclet jdiff.JDiff \
  -docletpath _lib/jdiff.jar:_lib/xerces-for-jdiff.jar \
  -oldapi "Guava $old" \
  -newapi "Guava $new" \
  -javadocold $oldjavadoc \
  -javadocnew $newjavadoc \
  -d $outputdir

# Make changes to the output
# Remove the useless user comments xml file
rm $outputdir/user_comments_for_Guava_*

# Change changes.html to index.html, making the url for a diff just /releases/<version>/api/diffs
mv $outputdir/changes.html $outputdir/index.html
# Change references to ../changes.html in the changes/ subdirectory  to reference the new URL (just ..)
find $outputdir/changes -name *.html -exec sed -i'.bak' -e 's#\.\./changes.html#..#g' {} ";"
# Just create backup files and then delete them... doesn't seem to be any good way to avoid this if
# you want this to work with either GNU or BSD sed.
rm $outputdir/changes/*.bak

# Cleanup
echo "Cleaning up temp files"
rm $tempoldxml
rm $tempnewxml

# Commit changes
echo "Committing changes"
git add .
git commit -m "Generate diffs between $old and $new"

echo "Finished"
