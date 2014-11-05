Guava: Google Core Libraries for Java
=====================================

[![Build Status](https://travis-ci.org/google/guava.svg?branch=master)](https://travis-ci.org/google/guava)

Requires JDK 1.6 or higher (as of 12.0).

Links
-----

- [GitHub project](https://github.com/google/guava)
- [Issue tracker: report a defect or feature request](https://github.com/google/guava/issues/new)
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
