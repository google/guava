#!/bin/bash

set -e -u

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <last version> <version>" >&2
  exit 1
fi

OLD="$1"
NEW="$2"

# cd to guava-libraries.docs:
cd "$(dirname "$0")"
cd ..

if git diff --name-only | grep . -q ; then
  echo "Uncommitted changes found. Aborting." >&2
  exit 1
fi

# TODO(cpovirk): test that guava-libraries and guava-libraries.docs branches match

find jdiff -type f -name '*.html' | xargs rm
cd jdiff
git rm user_comments_for_Guava_*
./jdiff.sh "$OLD" "$NEW"
find . -type f -name '*.html' -o -name '*.xml' | xargs git add
git commit -a -m "updated jdiff"
