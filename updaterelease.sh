#!/bin/bash

#***************************************************************************
#
# Main script for updating release API docs and diffs. Can be used to
# either update a specific release version or the current snapshot release.
#
# Usage examples:
#
#   ./updaterelease.sh snapshot
#   ./updaterelease.sh 18.0
#   ./updaterelease.sh 18.0-rc1
#
# All of these update the Javadoc located at releases/<release>/api/docs
# and the JDiff located at releases/<release>/api/diffs, creating those
# directories if this is a new release version. If <release> is 'snapshot',
# Javadoc and JDiff is derived from the 'master' branch. Otherwise, it is
# derived from the git tag 'v<release>'. In both cases, the actual version
# number is determined by checking out the git branch or tag and getting
# the version number from the pom.xml file via Maven (for non-snapshot
# releases, though, it should always be the same as the <release>
# argument).
#
# For all releases, JDiff compares the release to the previous non-rc
# release. For example, snapshot is compared against 18.0 (even if there's
# a 19.0-rc1 out) and 18.0 is compared against 17.0 (or 17.1 or 17.0.1 if
# there were one of those).
#
#***************************************************************************

set -e -u

# Ensure working dir is the root of the git repo and load util functions.
cd $(dirname $0)
source _util/util.sh

ensure_no_uncommitted_changes

# Ensure valid args from user and get the basic variables we need.
if [[ ! $# -eq 1 ]]; then
  echo "Usage: $0 <release>" >&2
  exit 1
fi
release=$1
releaseref="$(git_ref "$release")"
initialref="$(current_git_ref)"

# Create temp directories and files.
tempdir="$(mktemp -d -t "guava-$release-temp.XXX")"
logfile="$(mktemp -t "guava-$release-temp-log.XXX")"

# Ensure temp files are cleaned up and we're back on the original branch on exit.
function cleanup {
  exitcode=$?
  if [[ "$exitcode" == "0" ]]; then
    rm "$logfile"
  else
    # Put a newline in case we're in the middle of a "Do something... Done." line
    echo ""
    echo "Update failed: see log at '$logfile' for more details." >&2
    # If we failed while not on the original branch/ref, switch back to it.
    currentref="$(current_git_ref)"
    if [[ "$currentref" != "$initialref" ]]; then
      git checkout -q "$initialref"
    fi
  fi
  rm -fr "$tempdir"
  exit "$exitcode"
}
trap cleanup INT TERM EXIT

# Get the sed to use
sedbinary=sed
if [[ -n "$(which gsed)" ]]; then
  # default sed on OS X isn't GNU sed and behaves differently
  # use gsed if it's available
  sedbinary=gsed
fi

# Make sure we have all the latest tags
git fetch --tags


# Removes comments that cause unnecessary diffs from JDiff generated files,
# e.g. command line arguments and date generated.
function remove_unnecessary_comments {
  $sedbinary -i -r \
    -e '/^<!-- on .+ -->$/d' \
    -e '/^<!--\s+Command line arguments .+ -->$/d' $@
}

# Generates the Javadoc and JDiff for a Guava version.
function generate_docs {
  local dir="$1"

  # Switch to the git ref for the release to do things with the actual Guava repo.
  git_checkout_ref "$releaseref"

  if [[ -n "$dir" ]]; then
    if [[ ! -d "$dir" ]]; then
      echo "Subdirectory '$dir' does not exist for this version; skipping."
      git_checkout_ref "$initialref"
      return
    fi
    cd "$dir" &> /dev/null
  fi

  # Get the current Guava version from Maven.
  local guavaversion="$(guava_version)"

  if [[ -z "$dir" ]]; then
    # non-android version; set global variable
    GUAVA_VERSION="$guavaversion"
  fi

  echo "Updating Javadoc and JDiff for Guava $guavaversion"

  # Copy source files to a temp dir.
  cp -r guava/src "$tempdir/src"

  # Compile and generate Javadoc, putting class files in $tempdir/classes and docs in $tempdir/docs.

  echo -n "Compiling and generating Javadoc..."
  mvn \
    clean \
    compile \
    javadoc:javadoc \
    dependency:build-classpath \
    -Dmdep.outputFile="$tempdir/classpath" \
    -pl guava >> "$logfile" 2>&1
  echo " Done."

  mv guava/target/classes "$tempdir/classes"
  mv guava/target/site/apidocs "$tempdir/docs"

  # Create classpath string for JDiff to use.
  local classpath="$tempdir/classes:$(cat "$tempdir/classpath")"

  # Cleanup target dir.
  rm -fr guava/target

  if [[ -n "$dir" ]]; then
    cd - &> /dev/null
  fi

  # Switch back to gh-pages.
  git_checkout_ref "$initialref"

  # Generate JDiff XML file for the release.
  echo -n "Generating JDiff XML..."
  local jdiffpath="_util/lib/jdiff.jar:_util/lib/xerces-for-jdiff.jar"
  javadoc \
    -sourcepath "$tempdir/src" \
    -classpath "$classpath" \
    -subpackages com.google.common \
    -encoding UTF-8 \
    -doclet jdiff.JDiff \
    -docletpath "$jdiffpath" \
    -apiname "Guava $guavaversion" \
    -apidir "$tempdir" \
    -exclude com.google.common.base.internal \
    -protected >> "$logfile" 2>&1
  echo " Done."

  # Get the previous release version to diff against.
  echo -n "Determining previous release version..."
  local prevrelease="$(latest_release "$guavaversion")"

  mkdir "$tempdir/diffs"

  if [[ -z "$prevrelease" ]]; then
    echo " no previous release found."
  else
    echo " $prevrelease"

    cp "releases/$prevrelease/api/diffs/$prevrelease.xml" "$tempdir/Guava_$prevrelease.xml"

    # Generate Jdiff report, putting it in $tempdir/diffs

    # These are the base paths to Javadoc that will be used in the generated changes html files.
    # Use paths relative to the directory where those files will go (releases/$release/api/diffs/changes).
    # releases/$release/api/docs/
    releasejavadocpath="../../docs/"
    # releases/$prevrelease/api/docs/
    prevjavadocpath="../../../../$prevrelease/api/docs/"

    echo -n "Generating JDiff report between Guava $prevrelease and $guavaversion..."
    javadoc \
      -subpackages com \
      -doclet jdiff.JDiff \
      -docletpath "$jdiffpath" \
      -oldapi "Guava $prevrelease" \
      -oldapidir "$tempdir" \
      -newapi "Guava $guavaversion" \
      -newapidir "$tempdir" \
      -javadocold "$prevjavadocpath" \
      -javadocnew "$releasejavadocpath" \
      -d "$tempdir/diffs" >> "$logfile" 2>&1
    echo " Done."

    # Make changes to the JDiff output
    # Remove the useless user comments xml file
    rm $tempdir/diffs/user_comments_for_Guava_*

    # Change changes.html to index.html, making the url for a diff just releases/<release>/api/diffs/
    mv "$tempdir/diffs/changes.html" "$tempdir/diffs/index.html"
    # Change references to ../changes.html in the changes/ subdirectory  to reference the new URL (just ..)
    find "$tempdir/diffs/changes" -name "*.html" -exec "$sedbinary" -i -re 's#\.\./changes.html#..#g' {} ";"

    remove_unnecessary_comments "$tempdir/diffs/index.html"
  fi

  # Put the generated JDiff XML file in the correct place in the diffs dir.
  remove_unnecessary_comments "$tempdir/Guava_$guavaversion.xml"
  mv "$tempdir/Guava_$guavaversion.xml" "$tempdir/diffs/$guavaversion.xml"

  # Move generated output to the appropriate final directories.
  docsdir="releases/$guavaversion/api/docs"
  mkdir -p "$docsdir" && rm -fr "$docsdir"
  local diffsdir="releases/$guavaversion/api/diffs"
  mkdir -p "$diffsdir" && rm -fr "$diffsdir"

  echo -n "Moving generated Javadoc to $docsdir..."
  mv "$tempdir/docs" "$docsdir"
  echo " Done."

  echo -n "Moving generated JDiff to $diffsdir..."
  mv "$tempdir/diffs" "$diffsdir"
  echo " Done."

  rm -fr "$tempdir/*"
}

for dir in "" "android"; do
  generate_docs "$dir" "$releaseref"
done

git add . > /dev/null

# Commit
if ! git diff --cached --quiet ; then
  echo -n "Committing changes..."
  git commit -q -m "Generate Javadoc and JDiff for Guava $GUAVA_VERSION"
  echo " Done."
else
  echo "No changes to commit."
fi

# Update version info in _config.yml
if [[ $release == "snapshot" ]]; then
  fieldtoupdate="latest_snapshot"
  version="$GUAVA_VERSION"
else
  fieldtoupdate="latest_release"
  # The release being updated currently may not be the latest release.
  version="$(latest_release)"
fi

$sedbinary -i'' -re "s/$fieldtoupdate:[ ]+.+/$fieldtoupdate: $version/g" _config.yml

git add _config.yml > /dev/null

if ! git diff --cached --quiet ; then
  echo -n "Updating $fieldtoupdate in _config.yml to $version..."
  git commit -q -m "Update $fieldtoupdate version to $version"
  echo " Done."
fi

echo "Update succeeded."
