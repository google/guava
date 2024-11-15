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

set -eu

# Ensure working dir is the root of the git repo and load util functions.
cd "$(dirname $0)"
source _util/util.sh

ensure_no_uncommitted_changes

# Ensure valid args from user and get the basic variables we need.
if [[ ! $# -eq 1 ]]; then
  echo "Usage: $0 <release>" >&2
  exit 1
fi

readonly RELEASE="$1"
readonly RELEASE_REF="$(git_ref "$RELEASE")"
readonly INITIAL_REF="$(current_git_ref)"

# Create temp directories and files.
readonly TEMPDIR="$(mktemp -d -t "guava-$RELEASE-temp.XXX")"
readonly LOGFILE="$(mktemp -t "guava-$RELEASE-temp-log.XXX")"

readonly FLAVORS=("jre" "android")

readonly JDIFF_PATH="_util/lib/jdiff.jar:_util/lib/xerces-for-jdiff.jar"

# Ensure temp files are cleaned up and we're back on the original branch on exit.
function cleanup {
  exitcode=$?
  if [[ "$exitcode" != "0" ]]; then
    # Put a newline in case we're in the middle of a "Do something... Done." line
    echo ""
    echo "Update failed. Output of mvn/jdiff commands follows:" >&2
    echo >&2
    cat "$LOGFILE" >&2

    # If we failed while not on the original branch/ref, switch back to it.
    local currentref="$(current_git_ref)"
    if [[ "$currentref" != "$INITIAL_REF" ]]; then
      git checkout -q "$INITIAL_REF"
    fi
  fi
  rm "$LOGFILE"
  exit "$exitcode"
}
trap cleanup INT TERM EXIT

# Get the sed to use
SED=sed
if [[ -n "$(which gsed)" ]]; then
  # default sed on OS X isn't GNU sed and behaves differently
  # use gsed if it's available
  SED=gsed
fi

# Removes comments that cause unnecessary diffs from JDiff generated files,
# e.g. command line arguments and date generated.
remove_unnecessary_comments() {
  "$SED" -i -r \
    -e '/^<!-- on .+ -->$/d' \
    -e '/^<!--\s+Command line arguments .+ -->$/d' $@
}

# Generates Javadoc for the given flavor of Guava.
generate_javadoc() {
  local flavor="$1"

  # Change to android directory if necessary.
  if [[ "$flavor" == android ]]; then
    if [[ ! -d android ]]; then
      # If we're trying to build for the android flavor and the android
      # directory doesn't exist, just skip this.
      return
    fi
    cd android &> /dev/null
  fi

  mkdir -p "$TEMPDIR/$flavor"

  # Get the current Guava version from Maven.
  local version="$(guava_version)"
  echo "$version" > "$TEMPDIR/$flavor/VERSION"

  # Copy source files to the temp dir.
  cp -r guava/src "$TEMPDIR/$flavor/src"

  # Compile and generate Javadoc.
  # Save the build classpath to a file in the tempdir.
  echo -n "Compiling and generating Javadoc for Guava $version..."
  mvn \
    clean \
    compile \
    javadoc:javadoc \
    dependency:build-classpath \
    -Dmdep.outputFile="$TEMPDIR/$flavor/classpath" \
    -pl guava >> "$LOGFILE" 2>&1
  echo " Done."

  # Move generated .class and Javadoc files to the temp dir.
  mv guava/target/classes "$TEMPDIR/$flavor/classes"
  mv guava/target/reports/apidocs "$TEMPDIR/$flavor/docs"

  # Clean up target dir.
  rm -fr guava/target

  # If we changed directories, go back.
  if [[ "$flavor" == android ]]; then
    cd - &> /dev/null
  fi
}

# Generates the JDiff XML file for the given flavor.
generate_jdiff_xml() {
  local flavor="$1"

  if [[ ! -d "$TEMPDIR/$flavor" ]]; then
    return
  fi

  local classpath="$TEMPDIR/$flavor/classes:$(cat "$TEMPDIR/$flavor/classpath")"

  local version="$(cat "$TEMPDIR/$flavor/VERSION")"

  # Generate JDiff XML file for the release.
  echo -n "Generating JDiff XML for Guava $version..."
  ${JAVA_HOME}/bin/javadoc \
    -sourcepath "$TEMPDIR/$flavor/src" \
    -classpath "$classpath" \
    -subpackages com.google.common \
    -encoding UTF-8 \
    -doclet jdiff.JDiff \
    -docletpath "$JDIFF_PATH" \
    -apiname "Guava $version" \
    -apidir "$TEMPDIR/$flavor" \
    -exclude com.google.common.base.internal \
    -protected >> "$LOGFILE" 2>&1
  echo " Done."

  remove_unnecessary_comments "$TEMPDIR/$flavor/Guava_$version.xml"
  perl -pi -e 's/(?<=errorMessageArgs" type="Object)/.../' "$TEMPDIR/$flavor/Guava_$version.xml"
}

# Generates a JDiff report.
jdiff() {
  # The location of the root temporary directory for the "to" side where we want
  # to generate the report.
  local tempdir="$1"
  # The other version we're comparing against. We expect that the file
  # releases/$other_version/api/diffs/$other_version.xml exists.
  local other="$2"
  # The name of the directory (under the tempdir) where we'll put the diffs.
  local output_dir_name="$3"

  # If the other we're diffing against is "snapshot-jre", get its actual version
  # number to use. This should only happen when generating the diff between
  # snapshot-android and snapshot.
  local other_version="$other"
  if [[ "$other" == snapshot-jre ]]; then
    other_version="$(cat "$TEMPDIR/jre/VERSION")"
  fi

  local version="$(cat "$tempdir/VERSION")"

  local output_dir="$tempdir/$output_dir_name"
  mkdir "$output_dir"

  cp "releases/$other/api/diffs/$other.xml" "$tempdir/Guava_$other_version.xml"

  # These are the base paths to Javadoc that will be used in the generated changes html files.
  # Use paths relative to the directory where those files will go.
  local this_release_javadoc_path="../../docs/"
  local prev_release_javadoc_path="../../../../$other/api/docs/"

  echo -n "Generating JDiff report between Guava $other_version and $version..."
  ${JAVA_HOME}/bin/javadoc \
    -subpackages com \
    -doclet jdiff.JDiff \
    -docletpath "$JDIFF_PATH" \
    -oldapi "Guava $other_version" \
    -oldapidir "$tempdir" \
    -newapi "Guava $version" \
    -newapidir "$tempdir" \
    -javadocold "$prev_release_javadoc_path" \
    -javadocnew "$this_release_javadoc_path" \
    -d "$output_dir" >> "$LOGFILE" 2>&1
  echo " Done."

  # Make changes to the JDiff output.

  # Remove the useless user comments xml file.
  rm $output_dir/user_comments_for_Guava_*
  # Change changes.html to index.html, making the url for a diff just releases/<release>/api/diffs/
  mv "$output_dir/changes.html" "$output_dir/index.html"
  # Change references to ../changes.html in the changes/ subdirectory  to reference the new URL (just ..)
  find "$output_dir/changes" -name "*.html" -exec "$SED" -i -re 's#\.\./changes.html#..#g' {} ";"

  remove_unnecessary_comments "$output_dir/index.html"
}

# Generates the JDiff report comparing the current version to the previous
# release version for the given flavor.
jdiff_vs_previous_release() {
  local flavor="$1"

  if [[ ! -d "$TEMPDIR/$flavor" ]]; then
    return
  fi

  local version="$(cat "$TEMPDIR/$flavor/VERSION")"

  echo -n "Determining previous release version for Guava $version..."
  local prev_release="$(latest_release "$version")"

  if [[ "$flavor" == android ]] && [[ -z "$prev_release" ]]; then
    # This should really only matter for the first -android release. Diff it
    # against 20.0, which was the previous release that was runnable on Android.
    prev_release="20.0"
  fi

  if [[ -z "$prev_release" ]]; then
    # Probably shouldn't ever happen, but if it does... we can't generated a
    # diff.
    echo " no previous release found."
    return
  else
    echo "$prev_release"
  fi

  jdiff "$TEMPDIR/$flavor" "$prev_release" "diffs"
}

# Generates the JDiff report comparing the current android version to the
# current non-android version.
jdiff_android_vs_non_android() {
  local jre_version
  if [[ "$RELEASE" == "snapshot" ]]; then
    jre_version="snapshot-jre"
  else
    jre_version="$(cat "$TEMPDIR/jre/VERSION")"
  fi

  # The normal version has already been moved to its final location, so the
  # jdiff function will be able to find it the same way it finds a previous
  # version.
  jdiff "$TEMPDIR/android" "$jre_version" "androiddiffs"
}

# Given a source directory and target path, moves the source to the target path,
# ensuring that parent directories exist (if necessary) and that any existing
# directory at that path is deleted first.
clean_directory_move() {
  local src="$1"
  local target="$2"

  mkdir -p "$target"
  rm -fr "$target"
  mv "$src" "$target"
}

# Moves generated files for the given flavor to their proper location.
move_generated_files() {
  local flavor="$1"

  local version="$(cat "$TEMPDIR/$flavor/VERSION")"

  # Determine the name to use for this release; for non-snapshots, this is just
  # the version number. For snapshots, it should be either "snapshot" or
  # "snapshot-android". This name is used for the directory the results are put
  # in and for the JDiff XML file.
  local version_name="$version"
  if [[ "$RELEASE" == snapshot ]]; then
    version_name="snapshot-${flavor}"
  fi

  # Put the generated JDiff XML file in the correct place in the diffs dir.
  mv "$TEMPDIR/$flavor/Guava_$version.xml" "$TEMPDIR/$flavor/diffs/$version_name.xml"

  # Move generated output to the appropriate final directories.
  local releasedir="releases/$version_name"

  echo -n "Moving generated Javadoc to $releasedir/api/docs..."
  clean_directory_move "$TEMPDIR/$flavor/docs" "$releasedir/api/docs"
  echo " Done."

  echo -n "Moving generated JDiff to $releasedir/api/diffs..."
  clean_directory_move "$TEMPDIR/$flavor/diffs" "$releasedir/api/diffs"
  echo " Done."

  if [[ -d "$TEMPDIR/$flavor/androiddiffs" ]]; then
    # Android vs. non-android diffs exist for this (android) flavor... move them
    # too.
    echo -n "Moving generated Android JDiff to $releasedir/api/androiddiffs..."
    clean_directory_move "$TEMPDIR/$flavor/androiddiffs" "$releasedir/api/androiddiffs"
    echo " Done."
  fi
}

# Commits the generated Javadoc and JDiff to git.
commit_changes() {
  # Use the non-android version number from maven for the commit message
  local version="$(cat "$TEMPDIR/jre/VERSION")"

  git add -A > /dev/null

  # Commit
  if ! git diff --cached --quiet ; then
    echo -n "Committing changes..."
    git commit -q -m "Generate Javadoc and JDiff for Guava $version"
    echo " Done."
  else
    echo "No changes to commit."
  fi
}

# Updates the latest_release field in _config.yml if necessary.
update_config_yml() {
  # The release being updated currently may not be the latest release.
  version="$(latest_release)"
  # Remove the -jre suffix if present
  version=${version/-jre/}

  "$SED" -i'' -re "s/latest_release:[ ]+.+/latest_release: $version/g" _config.yml

  git add _config.yml > /dev/null

  if ! git diff --cached --quiet ; then
    echo -n "Updating latest_release in _config.yml to $version..."
    git commit -q -m "Update latest_release version to $version"
    echo " Done."
  fi
}

# Generates Javadoc shortlinks for the current snapshot release.
generate_snapshot_javadoc_shortlinks() {
  if [[ "$RELEASE" != "snapshot" ]]; then
    return
  fi
  # Creates 2 sub-folders for each non-nested class inside javadocshortcuts.
  # This ensures that each class documentation is accessible using  https://guava.dev/ClassName
  # or https://guava.dev/classname.
  for F in $(find releases/snapshot-jre/api/docs/com/google/common -name '[A-Z]*.html' -not -path '*/class-use/*' -not -name '*.*.*'); do
    SHORT=$(basename $F .html)

    # Lowercases the 2nd sub-folder's name
    SHORT_LOWER=$(echo $SHORT | tr A-Z a-z)
    mkdir -p javadocshortcuts/{$SHORT,$SHORT_LOWER} && (
      echo ---
      echo "title: $SHORT"
      echo "permalink: /$SHORT/"
      echo "redirect_to: https://guava.dev/$F"
      echo ---
    ) | tee javadocshortcuts/{$SHORT,$SHORT_LOWER}/index.md >/dev/null

    # Lowercases the permalink value inside the 2nd sub-folder's index.md
    perl -pi -e 's/^permalink.*/\L$&/' javadocshortcuts/$SHORT_LOWER/index.md
  done
}

# Main function actually run the process.
main() {
  if [[ -z "${JAVA_HOME:-}" ]]; then
    echo '$JAVA_HOME needs to be set' >&2
    exit 1
  elif [[ ! -x "${JAVA_HOME}/bin/javadoc" ]]; then
    echo '$JAVA_HOME must be set to a valid JDK location but is '"${JAVA_HOME}" >&2
    exit 1
  fi

  # Make sure we have all the latest tags
  git fetch --tags

  # Checkout the git ref (release tag or master) for the version of guava that
  # we're building docs for and do everything that needs to be done there. The
  # temp dir should contain the following after this is done:
  #
  # jre/
  #   VERSION    # file containing only the version number from Maven
  #   classpath  # file containing the classpath for building
  #   src/       # all guava srcs for the flavor
  #   classes/   # all generated .class files for the flavor
  #   docs/      # generated javadoc for the flavor
  # android/     # only if an android flavor exists for the version
  #   VERSION
  #   classpath
  #   src/
  #   classes/
  #   docs/
  git_checkout_ref "$RELEASE_REF"
  for flavor in "${FLAVORS[@]}"; do
    generate_javadoc "$flavor"
  done
  git_checkout_ref "$INITIAL_REF"

  # Back on gh-pages branch

  # Generate the JDiff XML file for each flavor and diff it against the previous
  # release for the flavor.
  for flavor in "${FLAVORS[@]}"; do
    generate_jdiff_xml "$flavor"
    jdiff_vs_previous_release "$flavor"
  done

  # Move the generated files for the non-android flavor only to their final
  # location. If there's an android flavor, there's one more thing we need to do
  # with it.
  move_generated_files "jre"

  if [[ -d "$TEMPDIR/android" ]]; then
    # If the android flavor exists for this version, we want to diff it against
    # the non-android version.
    jdiff_android_vs_non_android
    move_generated_files "android"
  fi

  generate_snapshot_javadoc_shortlinks
  commit_changes
  update_config_yml

  echo "Update succeeded."
}

main
