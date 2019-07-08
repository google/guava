This is a version of Guava that is annotated with type annotations for the Checker Framework.

The annotations are only in the main Guava project, not in the "Android" variant that supports JDK 7 and Android.


To build this project
---------------------

Optionally change `guava/pom.xml` to use a locally-built version of the Checker Framework.

To create file `guava/target/guava-HEAD-jre-SNAPSHOT.jar`:
(This takes about 25 minutes.)

```
(cd guava && mvn package -Dmaven.test.skip=true -Danimal.sniffer.skip=true)
```


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


Typechecking against the master branch of the Checker Framework
---------------------------------------------------------------

The released version of the Checker Framework may be incompatible with the
development version in the GitHub repository
https://github.com/typetools/checker-framework/tree/master .  For example,
the GitHub version may have renamed or added annotations or error messages.

When a pull request depends on forthcoming Checker Framework features, make
pull requests against a branch named `cf-master`.  Its Travis job uses the
Checker Framework from GitHub, rather than a Checker Framework release.

To create the `cf-master` branch if it does not exist:
It should differ only in file `.travis.yml`, which should pass
```
  "-P checkerframework-local"
```
(with the quotes) to `./.travis-build.sh`.

Whenever a Checker Framework release is made, undo the change in
`.travis.yml`, update the Checker Framework version number, pull-request
the `cf-master` branch into `master`, and delete the `cf-master` branch.


To update to a newer version of the upstream library
----------------------------------------------------

Pull in the latest Guava version; for example:
```
git fetch --tags https://github.com/google/guava
git pull https://github.com/google/guava v28.0
```

Use the latest Checker Framework version by changing `pom.xml` and `guava/pom.xml`.


To upload to Maven Central
--------------------------

This must be done on a CSE machine, which has access to the necessary passwords.

# Update the version number
#  * here, and
#  * in file guava/cfMavenCentral.xml.
# If it's not the same as the upstream version, then also edit pom.xml and guava/pom.xml.

PACKAGE=guava-28.0-jre

cd guava

# Compile, and create Javadoc jar file (`mvn clean` removes MANIFEST.MF).
# This takes about 20 minutes.
[ ! -z "$PACKAGE" ] && \
mvn clean && \
mvn package -Dmaven.test.skip=true -Danimal.sniffer.skip=true && \
mvn source:jar && \
mvn javadoc:javadoc && (cd target/site/apidocs && jar -cf ${PACKAGE}-javadoc.jar com)

## This does not seem to work for me:
# -Dhomedir=/projects/swlab1/checker-framework/hosting-info

[ ! -z "$PACKAGE" ] && \
mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=cfMavenCentral.xml -Dgpg.publicKeyring=/projects/swlab1/checker-framework/hosting-info/pubring.gpg -Dgpg.secretKeyring=/projects/swlab1/checker-framework/hosting-info/secring.gpg -Dgpg.keyname=ADF4D638 -Dgpg.passphrase="`cat /projects/swlab1/checker-framework/hosting-info/release-private.password`" -Dfile=target/${PACKAGE}.jar \
&& \
mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=cfMavenCentral.xml -Dgpg.publicKeyring=/projects/swlab1/checker-framework/hosting-info/pubring.gpg -Dgpg.secretKeyring=/projects/swlab1/checker-framework/hosting-info/secring.gpg -Dgpg.keyname=ADF4D638 -Dgpg.passphrase="`cat /projects/swlab1/checker-framework/hosting-info/release-private.password`" -Dfile=target/${PACKAGE}-sources.jar -Dclassifier=sources \
&& \
mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=cfMavenCentral.xml -Dgpg.publicKeyring=/projects/swlab1/checker-framework/hosting-info/pubring.gpg -Dgpg.secretKeyring=/projects/swlab1/checker-framework/hosting-info/secring.gpg -Dgpg.keyname=ADF4D638 -Dgpg.passphrase="`cat /projects/swlab1/checker-framework/hosting-info/release-private.password`" -Dfile=target/site/apidocs/${PACKAGE}-javadoc.jar -Dclassifier=javadoc

# Complete the release at https://oss.sonatype.org/#stagingRepositories
