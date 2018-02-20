This version of Guava contains additional type annotations for the Checker Framework.

To get updates from the original repository, run
```git pull https://github.com/google/guava```
and resolve merge conflicts where necessary.

To create file
`guava/target/guava-HEAD-jre-SNAPSHOT.jar`
run
  ```cd guava && mvn package -Dmaven.test.skip=true -Danimal.sniffer.skip=true```
(first, optionally change `guava/pom.xml` to use a locally-built version of the Checker Framework).
