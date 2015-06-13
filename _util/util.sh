#!/bin/bash

set -e -u

# Exits if there are uncommitted changes in the git repo.
function ensure_no_uncommitted_changes {
  if git diff --name-only | grep . -q ; then
    echo "Uncommitted changes found. Aborting." >&2
    exit 1
  fi
}

# Returns the current git branch, if any; the HEAD commit's SHA1 otherwise.
function current_git_ref {
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

# Checks that a given tag exists in the repo.
function check_tag_exists {
  tag=$1
  if ! git show-ref -q --verify refs/tags/$tag; then
    echo "Tag $tag does not exist" >&2
    exit 1
  fi
}

# Takes an input arg representing a version (as parsed in parse_version) and
# returns the git ref (tag or branch) that should be checked out to get that version.
function git_ref {
  release=$1
  if [[ $release == "snapshot" ]]; then
    echo "master"
  else
    tag="v$release"
    check_tag_exists $tag
    echo $tag
  fi
}

# Checks out the branch/ref with the given identifier.
# If the ref is the master branch, pulls to update it.
function git_checkout_ref {
  ref=$1
  echo -n "Checking out '$ref'..."
  git checkout -q $ref
  echo " Done."

  # If we're on master, pull to get the latest
  if [ $ref == "master" ]; then
    echo -n "Pulling to get latest changes..."
    git pull -q --ff-only
    echo " Done."
  fi
}

platform=$(uname)
if [[ $platform == "Linux" ]]; then
  # GNU utils
  extended="-r"
  versionsort="--version-sort"
else
  # BSD utils
  extended="-E"
  versionsort="-g"
fi

# Sorts all numeric releases from the _releases/ directory by version, from
# greatest version to least. This works as you'd expect, for example:
#
#   18.0.2 > 18.0.1 > 18.0 > 18.0-rc2 > 18.0-rc1 > 17.1 > 17.0.1 > 17.0
#
# This function expects to be run with the working directory at the root of
# the git tree.
function sort_releases {
  # This is all sorts of hacky and I'm sure there's a better way, but it
  # seems to work as long as we're just dealing with versions like 1.2,
  # 1.2.3, 1.2-rc1 and 1.2.3-rc1.
  ls _releases | \
      grep -E ^[0-9]+\.[0-9]+ | \
      sort -u | \
      sed $extended -e 's/^([0-9]+\.[0-9]+)$/\1.01/g' -e 's/-rc/!/g' | \
      sort -r $versionsort | \
      sed $extended -e 's/!/-rc/g' -e 's/\.01//g'
}

# Gets the major version part of a version number.
function major_version {
  majorversion=$(echo "$1" | cut -d . -f 1)
  if [[ ! $majorversion =~ ^[0-9]+$ ]]; then
    echo "Invalid version number: $1" >&2
    exit 1
  fi
  echo $majorversion
}

# Prints the highest non-rc release from the sorted list of releases
# produced by sort_releases. If a release argument is provided, print
# the highest non-rc release that has a major version that is lower than
# the given release. For example, given "16.0.1", return "15.0".
function latest_release {
  if [[ $# -eq 1 ]]; then
    ceiling=$(major_version "$1")
  else
    ceiling=""
  fi

  releases=$(sort_releases)
  for release in $releases; do
    if [[ ! -z "$ceiling" ]]; then
      releasemajor=$(major_version "$release")
      if (( ceiling <= releasemajor )); then
        continue
      fi
    fi
    if [[ ! $release =~ ^.+-rc[0-9]+$ ]]; then
      echo $release
      break
    fi
  done
}
