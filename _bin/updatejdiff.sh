#!/bin/bash

#********************************************************
#
# This script is only intended to be used for updating
# the API diffs for the Guava site, not for generating
# arbitrary diffs. Except when doing releases, it should
# generally be invoked as:
#
#   _bin/updatejdiff.sh 18.0 19.0-snapshot
#
# Using any -snapshot version as the new version will
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

javadoc \
  -subpackages com \
  -doclet jdiff.JDiff \
  -docletpath _lib/jdiff.jar:_lib/xerces-for-jdiff.jar \
  -oldapi "Guava $old" \
  -newapi "Guava $new" \
  -javadocold $oldjavadoc \
  -javadocnew $newjavadoc \
  -d $outputdir

rm $outputdir/user_comments_for_Guava_*
rm $tempoldxml
rm $tempnewxml
