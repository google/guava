#!/bin/bash

set -eu

if (( $# != 1 )); then
  echo "Usage: set_versions.sh <version>" >&2
  exit 1
fi

version="$1"
status="release"
if [[ $version == *"SNAPSHOT"* ]]; then
  status="integration"
fi

mvn versions:set versions:commit -DnewVersion="${version}-jre"
mvn versions:set versions:commit -DnewVersion="${version}-android" -f android
mvn versions:set-property -Dproperty=otherVariant.version -DnewVersion="${version}-android"
mvn versions:set-property -Dproperty=otherVariant.version -DnewVersion="${version}-jre" -f android
mvn versions:set-property -Dproperty=module.status -DnewVersion="${status}"
mvn versions:set-property -Dproperty=module.status -DnewVersion="${status}" -f android
mvn versions:set-property -Dproperty=module.parentVersion -DnewVersion="${version}-jre"
mvn versions:set-property -Dproperty=module.parentVersion -DnewVersion="${version}-jre" -f android
git commit -am "Set version numbers to ${version}"
