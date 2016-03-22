Guava: Google Core Libraries for Java
=====================================

[![Build Status](https://travis-ci.org/google/guava.svg?branch=master)](https://travis-ci.org/google/guava)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.google.guava/guava/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.google.guava/guava)

The Guava project contains several of Google's core libraries that we rely on
in our Java-based projects: collections, caching, primitives support,
concurrency libraries, common annotations, string processing, I/O, and so forth.

Requires JDK 1.6 or higher (as of 12.0).

Latest release
--------------

The most recent release is [Guava 19.0][], released December 9, 2015.

- 19.0 API Docs: [guava][guava-release-api-docs], [guava-testlib][testlib-release-api-docs]
- 19.0 API Diffs from 18.0: [guava][guava-release-api-diffs]

To add a dependency on Guava using Maven, use the following:

```xml
<dependency>
  <groupId>com.google.guava</groupId>
  <artifactId>guava</artifactId>
  <version>19.0</version>
</dependency>
```

To add a dependency using Gradle:

```
dependencies {
  compile 'com.google.guava:guava:19.0'
}
```

Snapshots
---------

Snapshots of Guava built from the `master` branch are available through Maven
using version `20.0-SNAPSHOT`. API documentation and diffs from version 19.0
are available here:

- Snapshot API Docs: [guava][guava-snapshot-api-docs]
- Snapshot API Diffs from 19.0: [guava][guava-snapshot-api-diffs]

Learn about Guava
------------------

- Our users' guide, [Guava Explained][]
- [Presentation slides focusing on base, primitives, and io](http://guava-libraries.googlecode.com/files/Guava_for_Netflix_.pdf)
- [Presentation slides focusing on cache]( http://guava-libraries.googlecode.com/files/JavaCachingwithGuava.pdf)
- [Presentation slides focusing on util.concurrent](http://guava-libraries.googlecode.com/files/guava-concurrent-slides.pdf)
- [A nice collection](http://www.tfnico.com/presentations/google-guava) of other helpful links

Links
-----

- [GitHub project](https://github.com/google/guava)
- [Issue tracker: Report a defect or feature request](https://github.com/google/guava/issues/new)
- [StackOverflow: Ask "how-to" and "why-didn't-it-work" questions](https://stackoverflow.com/questions/ask?tags=guava+java)
- [guava-discuss: For open-ended questions and discussion](http://groups.google.com/group/guava-discuss)

IMPORTANT WARNINGS
------------------

1. APIs marked with the `@Beta` annotation at the class or method level
are subject to change. They can be modified in any way, or even
removed, at any time. If your code is a library itself (i.e. it is
used on the CLASSPATH of users outside your own control), you should
not use beta APIs, unless you repackage them (e.g. using ProGuard).

2. Deprecated non-beta APIs will be removed two years after the
release in which they are first deprecated. You must fix your
references before this time. If you don't, any manner of breakage
could result (you are not guaranteed a compilation error).

3. Serialized forms of ALL objects are subject to change unless noted
otherwise. Do not persist these and assume they can be read by a
future version of the library.

4. Our classes are not designed to protect against a malicious caller.
You should not use them for communication between trusted and
untrusted code.

5. We unit-test and benchmark the libraries using only OpenJDK 1.7 on
Linux. Some features, especially in `com.google.common.io`, may not work
correctly in other environments.

[Guava 19.0]: https://github.com/google/guava/wiki/Release19
[guava-release-api-docs]: http://google.github.io/guava/releases/19.0/api/docs/
[testlib-release-api-docs]: http://www.javadoc.io/doc/com.google.guava/guava-testlib/19.0
[guava-release-api-diffs]: http://google.github.io/guava/releases/19.0/api/diffs/
[guava-snapshot-api-docs]: http://google.github.io/guava/releases/snapshot/api/docs/
[guava-snapshot-api-diffs]: http://google.github.io/guava/releases/snapshot/api/diffs/
[Guava Explained]: https://github.com/google/guava/wiki/Home
