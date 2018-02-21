This is a version of Guava with additional type annotations for the Checker Framework.

The annotations are only in the main Guava project, not in the "Android" variant that supports JDK 7 and Android.


To build this project
---------------------

First, optionally change `guava/pom.xml` to use a locally-built version of the Checker Framework

```
cd guava && mvn package -Dmaven.test.skip=true -Danimal.sniffer.skip=true
```

This creates file
`guava/target/guava-HEAD-jre-SNAPSHOT.jar`


To update to a newer version of the upstream library
----------------------------------------------------

In the upstream repository, find the commit corresponding to a public release.

Guava version 24.0 is commit 538d60aed09e945f59077770686df9cbd4e0048d

Pull in that commit:
```
git pull https://github.com/google/guava <commitid>
```

Update the PACKAGE environment variable below.


To upload to Maven Central
--------------------------

# Set a new Maven Central version number in file guava/cfMavenCentral.xml.

cd guava

PACKAGE=guava-23.5-jre

# Compile, and create Javadoc jar file
mvn package -Dmaven.test.skip=true -Danimal.sniffer.skip=true
mvn source:jar
mvn javadoc:javadoc && (cd target/site/apidocs && jar -cf ${PACKAGE}-javadoc.jar com)

## This does not seem to work for me:
# -Dhomedir=/projects/swlab1/checker-framework/hosting-info

mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=cfMavenCentral.xml -Dgpg.publicKeyring=/projects/swlab1/checker-framework/hosting-info/pubring.gpg -Dgpg.secretKeyring=/projects/swlab1/checker-framework/hosting-info/secring.gpg -Dgpg.keyname=ADF4D638 -Dgpg.passphrase="`cat /projects/swlab1/checker-framework/hosting-info/release-private.password`" -Dfile=target/${PACKAGE}.jar \
&& \
mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=cfMavenCentral.xml -Dgpg.publicKeyring=/projects/swlab1/checker-framework/hosting-info/pubring.gpg -Dgpg.secretKeyring=/projects/swlab1/checker-framework/hosting-info/secring.gpg -Dgpg.keyname=ADF4D638 -Dgpg.passphrase="`cat /projects/swlab1/checker-framework/hosting-info/release-private.password`" -Dfile=target/${PACKAGE}-sources.jar -Dclassifier=sources \
&& \
mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=cfMavenCentral.xml -Dgpg.publicKeyring=/projects/swlab1/checker-framework/hosting-info/pubring.gpg -Dgpg.secretKeyring=/projects/swlab1/checker-framework/hosting-info/secring.gpg -Dgpg.keyname=ADF4D638 -Dgpg.passphrase="`cat /projects/swlab1/checker-framework/hosting-info/release-private.password`" -Dfile=target/site/apidocs/${PACKAGE}-javadoc.jar -Dclassifier=javadoc
