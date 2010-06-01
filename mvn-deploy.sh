#!/bin/bash
#
# This script checks the java version and bails if it's less
# than Java6 (because we use @Override annotations on interface
# overriding methods.  It then proceeds to do a maven build that
# first cleans, then builds the normal lifecycle through compilation
# unit testing (if available) up to packaging.  It then packages
# the source, javadocs, and maven site.  It then signs the 
# artifacts with whatever pgp signature is the default of the 
# user executing it, and then deploys to the repository contained
# in the distributionManagement section of the pom.
#
# author: cgruber@google.com (Christian Edward Gruber)
#
if [[ -n ${JAVA_HOME} ]] ; then 
  JAVA_CMD=${JAVA_HOME}/bin/java
else
  JAVA_CMD=java
fi
java_version="$(${JAVA_CMD} -version 2>&1 | grep -e 'java version' | awk '{ print $3 }')"

# This test sucks, but it's short term
# TODO(cgruber) strip the quotes and patch version and do version comparison. 
greater_than_java5="$(echo ${java_version} | grep -e '^"1.[67]')"

if [[ -z ${greater_than_java5} ]] ; then
  echo "Your java version is ${java_version}."
  echo "You must use at least a java 6 JVM to build and deploy this software."
  exit 1
else
  echo "Building with java ${java_version}"
fi

if [[ $# > 0 ]]; then
  params+=" -Dgpg.keyname=${1}"
  gpg_sign_plugin=" gpg:sign"
fi
cmd="mvn clean package source:jar site:jar javadoc:jar ${gpg_sign_plugin} deploy ${params}"
echo "Executing ${cmd}"
${cmd}
