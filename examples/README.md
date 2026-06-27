A tiny examples module containing short, runnable demos of commonly-used Guava APIs:

- Immutable collections
- Multimap
- Multiset
- Optional
- Cache (LoadingCache example)
- RateLimiter

Build with Maven:
  mvn -f examples/pom.xml compile exec:java -Dexec.mainClass=com.example.guava.Examples
