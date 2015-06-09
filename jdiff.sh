#!/bin/sh
cd `dirname $0`

if [ "$#" -lt "1" ]
then
  echo "syntax: jdiff.sh <old_version>"
  exit 1
fi

prev=$1

latest=$2
if [ -n "$latest" ]
then
  latesturl="http://docs.guava-libraries.googlecode.com/git-history/v$latest/javadoc/"
else
  latest="latest"
  latesturl="../../javadoc/"
fi

javadoc \
  -classpath ../lib/jsr305.jar \
  -sourcepath ../src \
  -subpackages com \
  -doclet jdiff.JDiff \
  -docletpath ../lib/jdiff.jar:../lib/xerces-for-jdiff.jar \
  -oldapi "Guava $prev" \
  -newapi "Guava $latest" \
  -javadocold "http://docs.guava-libraries.googlecode.com/git-history/v$prev/javadoc/" \
  -javadocnew $latesturl \
  -d .
