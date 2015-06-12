#!/bin/bash

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
#
#   # Builds Javadoc for the 'v18.0' tag, commits to releases/18.0
#   _bin/updatejavadoc.sh 18.0
#
#******************************************************************************

set -e -u

cd $(dirname $0)/..
source _bin/util.sh

ensure_no_uncommitted_changes

if [[ $# -eq 0 ]]; then
  version="snapshot"
elif [[ $# -eq 1 ]]; then
  version=$(parse_version $1)
else
  echo "Usage: $0 [version]" >&2
  exit 1
fi

# Switch to the git ref for the target version
ghpagesref=$(currentref)
checkout $(git_ref $version)

# Make temp dir
doctemp=$(mktemp -d -t guava-$version-docs.XXX)

# Generate Javadoc and move it to temp dir
echo "Generating Javadoc"

# TODO(cgdecker): Redirect output to a temp log file and tell user about it if something goes wrong
mvn clean javadoc:javadoc -pl guava > /dev/null
echo "Moving Javadoc to '$doctemp'"
mv guava/target/site/apidocs/* $doctemp/
rm -fr guava/target

guavaversion=$(guava_version)

# Switch back to gh-pages branch
echo "Checking out '$ghpagesref'"
git checkout -q $ghpagesref

docsdir=releases/$version/api/docs

echo "Moving Javadoc to '$docsdir'"
mkdir -p $docsdir
rm -fr $docsdir
mv $doctemp $docsdir

echo "Committing changes"
git add .
git commit -m "Generate Javadoc for version $guavaversion"

echo "Finished"
