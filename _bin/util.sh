#!/bin/bash

# Exits if there are uncommitted changes in the git repo.
function ensure_no_uncommitted_changes {
  if git diff --name-only | grep . -q ; then
    echo "Uncommitted changes found. Aborting." >&2
    exit 1
  fi
}

# Returns the current git branch, if any; the HEAD commit's SHA1 otherwise.
function currentref {
  branch=$(git rev-parse --abbrev-ref HEAD)

  if [ $branch == "HEAD" ]; then
    echo $(git rev-parse HEAD)
  else
    echo $branch
  fi
}

# Returns the version of com.google.guava:guava at the current revision, pulled from Maven.
function guava_version {
  mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate \
      -Dexpression=project.version \
      -pl guava \
      | grep -Ev '(^\[|Download\w+:)'
}

# Takes an input arg and determines a version name for it.
# All of "master", "snapshot" and "<Anything>-SNAPSHOT" map to the "snapshot" version.
# Otherwise, the version must start with something of the form 18.0 (number.number).
function parse_version {
  version=$1
  if [[ $version =~ .+-SNAPSHOT ]] ||
     [[ $version == "master" ]]; then
    version="snapshot"
  elif [[ ! $version =~ [0-9]+\.[0-9]+.* ]] &&
       [[ ! $version == "snapshot" ]]; then
    echo "Invalid version specified: $version" >&2
    exit 1
  fi 

  echo $version
}

# Takes an input arg representing a version (as parsed in parse_version) and
# returns the git ref (tag or branch) that should be checked out to get that version.
function git_ref {
  version=$1
  if [[ $version == "snapshot" ]]; then
    echo "master"
  else
    echo "v$version"
  fi
}

# Checks out the branch/ref with the given identifier.
# If the ref is the master branch, pulls to update it.
function checkout {
  ref=$1
  echo "Checking out '$ref'"
  git checkout -q $ref

  # If we're on master, pull to get the latest
  if [ $ref == "master" ]; then
    echo "Pulling to get latest changes"
    git pull -q
  fi
}
