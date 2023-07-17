#!/bin/bash

set -e -u

# Exits if there are uncommitted changes in the git repo.
function ensure_no_uncommitted_changes {
  if ! git diff --quiet ; then
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
      -pl guava 2> /dev/null \
      | grep -Ev '(^\[|Download.+:)'
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
  if [[ $ref == "master" ]]; then
    echo -n "Pulling to get latest changes..."
    git pull -q --ff-only
    echo " Done."
  fi
}

# Expands a release number into a numeric release number than can be directly
# compared to another expanded release number. Release numbers without a patch
# release version get expanded to have a ".0" patch release number. Non RC
# releases all get a final ".1" while "-rcN" releases get ".0N", to ensure that
# RCs are always considered lower than final versions.
#
# If a release ends in "-android" or "-jre", the result will also end in that.
#
# Examples:
#
# - 22.0 -> 22.0.0.1
# - 22.0.1 -> 22.0.1.1
# - 22.1-rc1 -> 22.1.0.01
# - 22.1-rc2 -> 22.1.0.02
function expand_release {
  local release="$1"

  if [[ "$release" == "snapshot" ]]; then
    echo "$release"
    return
  fi

  # strip -SNAPSHOT suffix if present
  snapshot=""
  if [[ "$release" =~ ^(.+)-SNAPSHOT$ ]]; then
    release="${BASH_REMATCH[1]}"
    snapshot="-SNAPSHOT"
  fi

  # strip -final suffix if present
  final=""
  if [[ "$release" =~ ^(.+)-final$ ]]; then
    release="${BASH_REMATCH[1]}"
    final="-final"
  fi

  # strip -flavor suffix if present
  local flavor=""
  if [[ "$release" =~ ^(.+)-(jre|android)$ ]]; then
    release="${BASH_REMATCH[1]}"
    flavor="-${BASH_REMATCH[2]}"
  fi

  rc="100"
  if [[ "$release" =~ ^(.+)-rc([0-9]+)$ ]]; then
    # strip and record rc number if present
    release="${BASH_REMATCH[1]}"
    rc="0${BASH_REMATCH[2]}"
  fi

  if [[ "$release" =~ ^[0-9]+\.[0-9]+$ ]]; then
    release="$release.0"
  fi

  release="$release.$rc$final$flavor$snapshot"
  echo "$release"
}

# Converts an expanded release number back to its default form (no patch
# version if the patch version is 0 and the major version is less than 32,
# RCs converted back to using -rcN).
function unexpand_release {
  local release="$1"

  if [[ "$release" == "snapshot" ]]; then
    echo "$release"
    return
  fi

  # strip -SNAPSHOT suffix if present
  snapshot=""
  if [[ "$release" =~ ^(.+)-SNAPSHOT$ ]]; then
    release="${BASH_REMATCH[1]}"
    snapshot="-SNAPSHOT"
  fi

  # strip -final suffix if present
  final=""
  if [[ "$release" =~ ^(.+)-final$ ]]; then
    release="${BASH_REMATCH[1]}"
    final="-final"
  fi

  # strip -android suffix if present
  local flavor=""
  if [[ "$release" =~ ^(.+)-(jre|android)$ ]]; then
    release="${BASH_REMATCH[1]}"
    flavor="-${BASH_REMATCH[2]}"
  fi

  local release_array
  IFS='.' read -ra release_array <<< "$release"
  local major="${release_array[0]}"
  local minor="${release_array[1]}"
  local patch="${release_array[2]}"
  local rc="${release_array[3]}"

  local result="$major.$minor"
  if (( major >= 32 || patch != 0 )); then
    result="$result.$patch"
  fi

  if [[ "$rc" != "100" ]]; then
    rc="${rc:1}"
    result="$result-rc$rc"
  fi

  result="$result$final$flavor$snapshot"
  echo "$result"
}

function expand_releases {
  while read release; do
    expand_release "$release"
  done
}

function unexpand_releases {
  while read release; do
    unexpand_release "$release"
  done
}

platform="$(uname)"
if [[ "$platform" == "Linux" ]]; then
  # GNU utils
  extended="-r"
  versionsort="--version-sort"
else
  # BSD utils
  extended="-E"
  versionsort="-g"
fi

# Sorts all numeric releases given on stdin by version, from greatest version to
# least. This works as you'd expect, for example:
#
#   18.0.2 > 18.0.1 > 18.0 > 18.0-rc2 > 18.0-rc1 > 17.1 > 17.0.1 > 17.0
function sort_releases {
  expand_releases | sort -r $versionsort | unexpand_releases
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

# Ceiling to use when looking for the previous version for a HEAD snapshot.
# This should be safe to ensure it's higher than any Guava version for a
# while. =)
readonly SNAPSHOT_CEILING="999.999.999"

# Prints the highest non-rc release from the sorted list of releases produced
# by sort_releases. If a release argument is provided, print the highest non-rc
# release that has a major or minor version that is lower than the given
# release. For example, given "16.0.1", return "15.0". Given "16.1", return
# "16.0.1".
#
# If the release argument is a "-android" release, only look at other
# "-android" releases.
function latest_release {
  if [[ $# == 1 ]]; then
    local ceiling="$1"
    if [[ "$ceiling" =~ ^HEAD-(jre|android)-SNAPSHOT$ ]]; then
      ceiling="${SNAPSHOT_CEILING}-${BASH_REMATCH[1]}"
    fi
  else
    local ceiling="${SNAPSHOT_CEILING}"
  fi
  local non_rc_releases="$(ls releases | grep -v "rc" | grep -v "snapshot")"

  if [[ "$ceiling" =~ ^.+-android(-SNAPSHOT)?$ ]]; then
    # If the release we're looking at is an android release, only look at other
    # android releases.
    non_rc_releases="$(echo "$non_rc_releases" | grep -e "-android")"
  else
    # If it's not an android release, don't include android releases.
    non_rc_releases="$(echo "$non_rc_releases" | grep -v -e "-android")"
  fi

  if [[ -z "$non_rc_releases" ]]; then
    # There are no non-RC releases to compare against (or no android releases).
    # Print nothing because there is no previous release.
    return
  fi

  # Add the release we're looking for to the list, uniqueify, then sort
  # according to our release sort order.
  local releases="$((echo "$non_rc_releases" && echo "$ceiling") | sort -u | sort_releases)"

  ceiling_expanded="$(expand_release "$ceiling")"
  ceiling_major="$(cut -d. -f1 <<< "$ceiling_expanded")"
  ceiling_minor="$(cut -d. -f2 <<< "$ceiling_expanded")"

  local release
  local seen_ceiling=1
  for release in $releases; do
    if [[ "$seen_ceiling" ]]; then
      release_expanded="$(expand_release "$release")"
      release_major="$(cut -d. -f1 <<< "$release_expanded")"
      release_minor="$(cut -d. -f2 <<< "$release_expanded")"

      if (( release_major < ceiling_major || release_minor < ceiling_minor )); then
        echo "$release"
        return
      fi
    elif [[ "$release" == "$ceiling" ]]; then
      seen_ceiling=0
    fi
  done
}
