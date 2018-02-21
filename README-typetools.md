This version of Guava contains additional type annotations for the Checker Framework.

To get updates from the original repository, choose a released version, run
```
git pull https://github.com/google/guava <commitid>
```
and resolve merge conflicts where necessary.
Guava version 24.0 is commit 538d60aed09e945f59077770686df9cbd4e0048d

To create file
`guava/target/guava-HEAD-jre-SNAPSHOT.jar`
run
  ```cd guava && mvn package -Dmaven.test.skip=true -Danimal.sniffer.skip=true```
(first, optionally change `guava/pom.xml` to use a locally-built version of the Checker Framework).


To upload to Maven Central:

TODO
