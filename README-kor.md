# Guava : Java 용 Google Core Libraries

[![Latest release](https://img.shields.io/github/release/google/guava.svg)](https://github.com/google/guava/releases/latest)
[![Build Status](https://travis-ci.org/google/guava.svg?branch=master)](https://travis-ci.org/google/guava)

Guava는 새로운 컬렉션 유형 (예: 멀티 맵 및 멀티 세트), 변경 불가능한 컬렉션, 그래프 라이브러리 및 동시성, I/O, 해싱, 프리미티브, 문자열 등을
위한 유틸리티를 포함하는 핵심 라이브러리 세트입니다.  

Guava는 두 가지 풍미(flavor)가 있습니다.

*   JRE flavor느 JDK 1.8 혹은 그 이상이 필요합니다.
*   JDK 1.7 또는 Android를 지원해야하는 경우 Android flavor를 사용하십시오. [`android` directory] 에서 Android Guava 소스를 찾을 수 있습니다.

[`android` directory]: https://github.com/google/guava/tree/master/android

## 빌드에 Guava 추가

Guava의 Maven 그룹 ID는 `com.google.guava`이고, 이것의 아티팩트 ID는 `guava`. 입니다.
Guava는 두 가지의 다른 "풍미(flavor)"를 제공합니다.
하나는 (Java 8+) JRE에서 사용하기 위한 것이고 다른 하나는 Android 또는 Java 7에서 사용하기 위한 것입니다.  
이 특징은 Maven 버전 필드에 `28.1-jre` 또는 `28.1-android`로 지정됩니다. Guava에 대한 자세한 내용은  [using Guava in your build]. 를 참조하십시오.

Maven을 사용하여 Guava에 대한 종속성을 추가하려면 다음을 사용하십시오:

```xml
<dependency>
  <groupId>com.google.guava</groupId>
  <artifactId>guava</artifactId>
  <version>28.1-jre</version>
  <!-- or, for Android: -->
  <version>28.1-android</version>
</dependency>
```

Gradle을 사용하여 종속성을 추가하려면:

```gradle
dependencies {
  compile 'com.google.guava:guava:28.1-jre'
  // or, for Android:
  api 'com.google.guava:guava:28.1-android'
}
```

## Snapshots

`master` branch에서 빌드된 Guava의 Snapshots은 Android flavor를 위해 `HEAD-jre-SNAPSHOT` 혹은 
`HEAD-android-SNAPSHOT`의 버젼을 이용한 Maven을 통해 사용이 가능합니다.


- Snapshot API 문서: [guava][guava-snapshot-api-docs]
- Snapshot API 차이: [guava][guava-snapshot-api-diffs]

## Guava에 대해 알아보기

- 사용자 안내서, [Guava Explained]
- [다른 유용한 링크](http://www.tfnico.com/presentations/google-guava)

## Links

- [GitHub 프로젝트](https://github.com/google/guava)
- [Issue tracker: 결함 또는 기능 요청보고](https://github.com/google/guava/issues/new)
- [StackOverflow:"방법" 및 "왜 작동하지 않는지" 질문](https://stackoverflow.com/questions/ask?tags=guava+java)
- [guava-announce: release 발표 및 예정된 중요한 변경 사항](http://groups.google.com/group/guava-announce)
- [guava-discuss: 개방형 질문 및 토론](http://groups.google.com/group/guava-discuss)

## 중요 경고

1. `@Beta`클래스 또는 메소드 레벨에서 주석이 표시된 API는 변경 될 수 있습니다. 언제든지 수정하거나 제거 할 수 있습니다. 
코드가 라이브러리 자체 인 경우 (즉, 자신의 컨트롤 외부에있는 사용자의 CLASSPATH에서 사용되는 경우) 
[repackage] 하지 않는 한 베타 API를 사용해서는 안됩니다. **코드가 라이브러리 인 경우 API를 사용하지 않도록 [Guava Beta Checker] 를 
사용하는 것이 좋습니다 `@Beta`.**

1. APIs marked with the `@Beta` annotation at the class or method level
are subject to change. They can be modified in any way, or even
removed, at any time. If your code is a library itself (i.e. it is
used on the CLASSPATH of users outside your own control), you should
not use beta APIs, unless you [repackage] them. **If your
code is a library, we strongly recommend using the [Guava Beta Checker] to
ensure that you do not use any `@Beta` APIs!**

2. APIs without `@Beta` will remain binary-compatible for the indefinite
future. (Previously, we sometimes removed such APIs after a deprecation period.
The last release to remove non-`@Beta` APIs was Guava 21.0.) Even `@Deprecated`
APIs will remain (again, unless they are `@Beta`). We have no plans to start
removing things again, but officially, we're leaving our options open in case
of surprises (like, say, a serious security problem).

3. Guava has one dependency that is needed at runtime:
`com.google.guava:failureaccess:1.0.1`

4. Serialized forms of ALL objects are subject to change unless noted
otherwise. Do not persist these and assume they can be read by a
future version of the library.

5. Our classes are not designed to protect against a malicious caller.
You should not use them for communication between trusted and
untrusted code.

6. For the mainline flavor, we unit-test the libraries using only OpenJDK 1.8 on
Linux. Some features, especially in `com.google.common.io`, may not work
correctly in other environments. For the Android flavor, our unit tests run on
API level 15 (Ice Cream Sandwich).

[guava-snapshot-api-docs]: https://google.github.io/guava/releases/snapshot-jre/api/docs/
[guava-snapshot-api-diffs]: https://google.github.io/guava/releases/snapshot-jre/api/diffs/
[Guava Explained]: https://github.com/google/guava/wiki/Home
[Guava Beta Checker]: https://github.com/google/guava-beta-checker

<!-- References -->

[using Guava in your build]: https://github.com/google/guava/wiki/UseGuavaInYourBuild
[repackage]: https://github.com/google/guava/wiki/UseGuavaInYourBuild#what-if-i-want-to-use-beta-apis-from-a-library-that-people-use-as-a-dependency
