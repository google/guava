This is a version of Guava with additional type annotations for the Checker Framework.

The annotations are only in the main Guava project, not in the "Android" variant that supports JDK 7 and Android.


To build this project
---------------------

Optionally change `guava/pom.xml` to use a locally-built version of the Checker Framework

Create file `guava/target/guava-HEAD-jre-SNAPSHOT.jar`:

```
(cd guava && mvn package -Dmaven.test.skip=true -Danimal.sniffer.skip=true)
```


To update to a newer version of the upstream library
----------------------------------------------------

This must be done on a CSE machine, which has access to the necessary passwords.

In the upstream repository, find the commit corresponding to a public release.

Date of release: https://github.com/google/guava/releases
Commits: https://github.com/google/guava/commits/master

Guava version 24.0 is commit 538d60aed09e945f59077770686df9cbd4e0048d
Guava version 24.1 is commit 444ff98e688b384e73d7b599b4168fed8003eb3f
Guava version 25.0 is commit 2cac83e70d77f0fa9b2352fe5ac994280fc3b028

Pull in that commit:
```
git fetch https://github.com/google/guava
git pull https://github.com/google/guava <commitid>
```

Change pom.xml files that have the most recent Guava release hard-coded:

```
preplace '24\.1' 25.0 `findfile pom.xml` guava/cfMavenCentral.xml
```

Use latest Checker Framework version by changing `pom.xml` and `guava/pom.xml`.


To upload to Maven Central
--------------------------

# Ensure the version number is set properly in file guava/cfMavenCentral.xml.
# Then, set this variable to the same version.
PACKAGE=guava-25.0-jre

cd guava


# Compile, and create Javadoc jar file
mvn package -Dmaven.test.skip=true -Danimal.sniffer.skip=true && \
mvn source:jar && \
mvn javadoc:javadoc && (cd target/site/apidocs && jar -cf ${PACKAGE}-javadoc.jar com)

## This does not seem to work for me:
# -Dhomedir=/projects/swlab1/checker-framework/hosting-info

mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=cfMavenCentral.xml -Dgpg.publicKeyring=/projects/swlab1/checker-framework/hosting-info/pubring.gpg -Dgpg.secretKeyring=/projects/swlab1/checker-framework/hosting-info/secring.gpg -Dgpg.keyname=ADF4D638 -Dgpg.passphrase="`cat /projects/swlab1/checker-framework/hosting-info/release-private.password`" -Dfile=target/${PACKAGE}.jar \
&& \
mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=cfMavenCentral.xml -Dgpg.publicKeyring=/projects/swlab1/checker-framework/hosting-info/pubring.gpg -Dgpg.secretKeyring=/projects/swlab1/checker-framework/hosting-info/secring.gpg -Dgpg.keyname=ADF4D638 -Dgpg.passphrase="`cat /projects/swlab1/checker-framework/hosting-info/release-private.password`" -Dfile=target/${PACKAGE}-sources.jar -Dclassifier=sources \
&& \
mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=cfMavenCentral.xml -Dgpg.publicKeyring=/projects/swlab1/checker-framework/hosting-info/pubring.gpg -Dgpg.secretKeyring=/projects/swlab1/checker-framework/hosting-info/secring.gpg -Dgpg.keyname=ADF4D638 -Dgpg.passphrase="`cat /projects/swlab1/checker-framework/hosting-info/release-private.password`" -Dfile=target/site/apidocs/${PACKAGE}-javadoc.jar -Dclassifier=javadoc

# Browse to https://oss.sonatype.org/#stagingRepositories to complete the release.

Typechecking
------------

Only the packages `com.google.common.primitives` and `com.google.common.base` are annotated by Index Checker annotations. 
In order to get implicit annotations in class files, the Index Checker runs on all files during compilation, but warnings are suppressed. The Index Checker is run in another phase to typecheck just the two annotated packages. If there are errors, then the build fails.

The Maven properties in guava/pom.xml can be used to change the behavior:

- `checkerframework.checkers` defines which checkers are run during compilation
- `checkerframework.suppress` defines warning keys suppressed during compilation
- `checkerframework.index.packages` defines packages checked by the Index Checker 

- `checkerframework.extraargs` defines additional argument passed to the checkers during compilation, for example `-Ashowchecks`.
- `checkerframework.extraargs2` defines additional argument passed to the checkers during compilation, for example `-Aannotations`.
- `index.only.arg` defines additional argument passed to the Index Checker, for example `-Ashowchecks`.
