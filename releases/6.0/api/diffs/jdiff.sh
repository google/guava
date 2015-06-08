#!/bin/sh
cd `dirname $0`

if [ "$#" != "1" ]
then
  echo "die"
  exit 1
fi

prev=$1

cp ../../../release$prev/javadoc/jdiff/Guava_r$prev.xml .

javadoc \
  -classpath ../../lib/jsr305.jar \
  -sourcepath ../../src \
  -subpackages com \
  -doclet jdiff.JDiff \
  -docletpath ../../lib/jdiff.jar:../../lib/xerces-for-jdiff.jar \
  -oldapi "Guava r$prev" \
  -newapi "Guava r06" \
  -javadocold http://guava-libraries.googlecode.com/svn/tags/release$prev/javadoc/ \
  -javadocnew http://guava-libraries.googlecode.com/svn/tags/release06/javadoc/ \
  -d .

rm Guava_r$prev.xml

