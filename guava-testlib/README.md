# Guava Testlib: Google Testing Libraries for Java

Guava Testlib is a set of utilities for writing JUnit tests.

## Adding Guava Testlib to your build

Guava's Maven group ID is `com.google.guava` and its artifact ID is `guava-testlib`.

To add a dependency on Guava using Maven, use the following:

```xml
<dependency>
  <groupId>com.google.guava</groupId>
  <artifactId>guava-testlib</artifactId>
  <version>27.1-jre</version>
  <scope>test</scope>
</dependency>
```

To add a dependency using Gradle:

```gradle
dependencies {
  test 'com.google.guava:guava-testlib:27.1-jre'
}
```

## Links

- [GitHub project](https://github.com/google/guava)
- [Issue tracker: Report a defect or feature request](https://github.com/google/guava/issues/new)
- [StackOverflow: Ask "how-to" and "why-didn't-it-work" questions](https://stackoverflow.com/questions/ask?tags=guava+java)
- [guava-discuss: For open-ended questions and discussion](http://groups.google.com/group/guava-discuss)

## IMPORTANT WARNINGS

1. APIs marked with the `@Beta` annotation at the class or method level
are subject to change. They can be modified in any way, or even
removed, at any time. If your code is a library itself (i.e. it is
used on the CLASSPATH of users outside your own control), you should
not use beta APIs, unless you [repackage] them. **If your
code is a library, we strongly recommend using the [Guava Beta Checker] to
ensure that you do not use any `@Beta` APIs!**

