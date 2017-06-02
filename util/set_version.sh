#!/bin/bash

set -eu

if (( $# != 1 )); then
  echo "Usage: set_versions.sh <version>" >&2
  exit 1
fi

version="$1"
if [[ "${version}" =~ ^(.+)-SNAPSHOT$ ]]; then
  android_version="${BASH_REMATCH[1]}-android-SNAPSHOT"
else
  android_version="${version}-android"
fi

mvn versions:set versions:commit -DnewVersion="${version}"
mvn versions:set versions:commit -DnewVersion="${android_version}" \
  -f android
git commit -am "Set version numbers to ${version}"
