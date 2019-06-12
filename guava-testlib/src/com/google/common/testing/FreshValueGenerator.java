/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.testing;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.throwIfUnchecked;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Equivalence;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Ticker;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.RowSortedTable;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.common.collect.TreeMultiset;
import com.google.common.primitives.Primitives;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.Reflection;
import com.google.common.reflect.TypeToken;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Generates fresh instances of types that are different from each other (if possible).
 *
 * @author Ben Yu
 */
@GwtIncompatible
class FreshValueGenerator {

  private static final ImmutableMap<Class<?>, Method> GENERATORS;

  static {
    ImmutableMap.Builder<Class<?>, Method> builder = ImmutableMap.builder();
    for (Method method : FreshValueGenerator.class.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Generates.class)) {
        builder.put(method.getReturnType(), method);
      }
    }
    GENERATORS = builder.build();
  }

  private static final ImmutableMap<Class<?>, Method> EMPTY_GENERATORS;

  static {
    ImmutableMap.Builder<Class<?>, Method> builder = ImmutableMap.builder();
    for (Method method : FreshValueGenerator.class.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Empty.class)) {
        builder.put(method.getReturnType(), method);
      }
    }
    EMPTY_GENERATORS = builder.build();
  }

  private final AtomicInteger freshness = new AtomicInteger(1);
  private final ListMultimap<Class<?>, Object> sampleInstances = ArrayListMultimap.create();

  /**
   * The freshness level at which the {@link Empty @Empty} annotated method was invoked to generate
   * instance.
   */
  private final Map<Type, Integer> emptyInstanceGenerated = new HashMap<>();

  final <T> void addSampleInstances(Class<T> type, Iterable<? extends T> instances) {
    sampleInstances.putAll(checkNotNull(type), checkNotNull(instances));
  }

  /**
   * Returns a fresh instance for {@code type} if possible. The returned instance could be:
   *
   * <ul>
   *   <li>exactly of the given type, including generic type parameters, such as {@code
   *       ImmutableList<String>};
   *   <li>of the raw type;
   *   <li>null if no value can be generated.
   * </ul>
   */
  @Nullable
  final Object generateFresh(TypeToken<?> type) {
    Object generated = generate(type);
    if (generated != null) {
      freshness.incrementAndGet();
    }
    return generated;
  }

  @Nullable
  final <T> T generateFresh(Class<T> type) {
    return Primitives.wrap(type).cast(generateFresh(TypeToken.of(type)));
  }

  final <T> T newFreshProxy(final Class<T> interfaceType) {
    T proxy = newProxy(interfaceType);
    freshness.incrementAndGet();
    return proxy;
  }

  /**
   * Generates an instance for {@code type} using the current {@link #freshness}. The generated
   * instance may or may not be unique across different calls.
   */
  private Object generate(TypeToken<?> type) {
    Class<?> rawType = type.getRawType();
    List<Object> samples = sampleInstances.get(rawType);
    Object sample = pickInstance(samples, null);
    if (sample != null) {
      return sample;
    }
    if (rawType.isEnum()) {
      return pickInstance(rawType.getEnumConstants(), null);
    }
    if (type.isArray()) {
      TypeToken<?> componentType = type.getComponentType();
      Object array = Array.newInstance(componentType.getRawType(), 1);
      Array.set(array, 0, generate(componentType));
      return array;
    }
    Method emptyGenerate = EMPTY_GENERATORS.get(rawType);
    if (emptyGenerate != null) {
      if (emptyInstanceGenerated.containsKey(type.getType())) {
        // empty instance already generated
        if (emptyInstanceGenerated.get(type.getType()).intValue() == freshness.get()) {
          // same freshness, generate again.
          return invokeGeneratorMethod(emptyGenerate);
        } else {
          // Cannot use empty generator. Proceed with other generators.
        }
      } else {
        // never generated empty instance for this type before.
        Object emptyInstance = invokeGeneratorMethod(emptyGenerate);
        emptyInstanceGenerated.put(type.getType(), freshness.get());
        return emptyInstance;
      }
    }
    Method generate = GENERATORS.get(rawType);
    if (generate != null) {
      ImmutableList<Parameter> params = Invokable.from(generate).getParameters();
      List<Object> args = Lists.newArrayListWithCapacity(params.size());
      TypeVariable<?>[] typeVars = rawType.getTypeParameters();
      for (int i = 0; i < params.size(); i++) {
        TypeToken<?> paramType = type.resolveType(typeVars[i]);
        // We require all @Generates methods to either be parameter-less or accept non-null
        // values for their generic parameter types.
        Object argValue = generate(paramType);
        if (argValue == null) {
          // When a parameter of a @Generates method cannot be created,
          // The type most likely is a collection.
          // Our distinct proxy doesn't work for collections.
          // So just refuse to generate.
          return null;
        }
        args.add(argValue);
      }
      return invokeGeneratorMethod(generate, args.toArray());
    }
    return defaultGenerate(rawType);
  }

  private <T> T defaultGenerate(Class<T> rawType) {
    if (rawType.isInterface()) {
      // always create a new proxy
      return newProxy(rawType);
    }
    return ArbitraryInstances.get(rawType);
  }

  private <T> T newProxy(final Class<T> interfaceType) {
    return Reflection.newProxy(interfaceType, new FreshInvocationHandler(interfaceType));
  }

  private Object invokeGeneratorMethod(Method generator, Object... args) {
    try {
      return generator.invoke(this, args);
    } catch (InvocationTargetException e) {
      throwIfUnchecked(e.getCause());
      throw new RuntimeException(e.getCause());
    } catch (Exception e) {
      throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  private final class FreshInvocationHandler extends AbstractInvocationHandler {
    private final int identity = generateInt();
    private final Class<?> interfaceType;

    FreshInvocationHandler(Class<?> interfaceType) {
      this.interfaceType = interfaceType;
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) {
      return interfaceMethodCalled(interfaceType, method);
    }

    @Override
    public int hashCode() {
      return identity;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof FreshInvocationHandler) {
        FreshInvocationHandler that = (FreshInvocationHandler) obj;
        return identity == that.identity;
      }
      return false;
    }

    @Override
    public String toString() {
      return paramString(interfaceType, identity);
    }
  }

  /** Subclasses can override to provide different return value for proxied interface methods. */
  Object interfaceMethodCalled(Class<?> interfaceType, Method method) {
    throw new UnsupportedOperationException();
  }

  private <T> T pickInstance(T[] instances, T defaultValue) {
    return pickInstance(Arrays.asList(instances), defaultValue);
  }

  private <T> T pickInstance(Collection<T> instances, T defaultValue) {
    if (instances.isEmpty()) {
      return defaultValue;
    }
    // generateInt() is 1-based.
    return Iterables.get(instances, (generateInt() - 1) % instances.size());
  }

  private static String paramString(Class<?> type, int i) {
    return type.getSimpleName() + '@' + i;
  }

  /**
   * Annotates a method to be the instance generator of a certain type. The return type is the
   * generated type. The method parameters correspond to the generated type's type parameters. For
   * example, if the annotated method returns {@code Map<K, V>}, the method signature should be:
   * {@code Map<K, V> generateMap(K key, V value)}.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  private @interface Generates {}

  /**
   * Annotates a method to generate the "empty" instance of a collection. This method should accept
   * no parameter. The value it generates should be unequal to the values generated by methods
   * annotated with {@link Generates}.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  private @interface Empty {}

  @Generates
  private Class<?> generateClass() {
    return pickInstance(
        ImmutableList.of(
            int.class, long.class, void.class, Object.class, Object[].class, Iterable.class),
        Object.class);
  }

  @Generates
  private Object generateObject() {
    return generateString();
  }

  @Generates
  private Number generateNumber() {
    return generateInt();
  }

  @Generates
  private int generateInt() {
    return freshness.get();
  }

  @Generates
  private Integer generateInteger() {
    return new Integer(generateInt());
  }

  @Generates
  private long generateLong() {
    return generateInt();
  }

  @Generates
  private Long generateLongObject() {
    return new Long(generateLong());
  }

  @Generates
  private float generateFloat() {
    return generateInt();
  }

  @Generates
  private Float generateFloatObject() {
    return new Float(generateFloat());
  }

  @Generates
  private double generateDouble() {
    return generateInt();
  }

  @Generates
  private Double generateDoubleObject() {
    return new Double(generateDouble());
  }

  @Generates
  private short generateShort() {
    return (short) generateInt();
  }

  @Generates
  private Short generateShortObject() {
    return new Short(generateShort());
  }

  @Generates
  private byte generateByte() {
    return (byte) generateInt();
  }

  @Generates
  private Byte generateByteObject() {
    return new Byte(generateByte());
  }

  @Generates
  private char generateChar() {
    return generateString().charAt(0);
  }

  @Generates
  private Character generateCharacter() {
    return new Character(generateChar());
  }

  @Generates
  private boolean generateBoolean() {
    return generateInt() % 2 == 0;
  }

  @Generates
  private Boolean generateBooleanObject() {
    return new Boolean(generateBoolean());
  }

  @Generates
  private UnsignedInteger generateUnsignedInteger() {
    return UnsignedInteger.fromIntBits(generateInt());
  }

  @Generates
  private UnsignedLong generateUnsignedLong() {
    return UnsignedLong.fromLongBits(generateLong());
  }

  @Generates
  private BigInteger generateBigInteger() {
    return BigInteger.valueOf(generateInt());
  }

  @Generates
  private BigDecimal generateBigDecimal() {
    return BigDecimal.valueOf(generateInt());
  }

  @Generates
  private CharSequence generateCharSequence() {
    return generateString();
  }

  @Generates
  private String generateString() {
    return Integer.toString(generateInt());
  }

  @Generates
  private Comparable<?> generateComparable() {
    return generateString();
  }

  @Generates
  private Pattern generatePattern() {
    return Pattern.compile(generateString());
  }

  @Generates
  private Charset generateCharset() {
    return pickInstance(Charset.availableCharsets().values(), Charsets.UTF_8);
  }

  @Generates
  private Locale generateLocale() {
    return pickInstance(Locale.getAvailableLocales(), Locale.US);
  }

  @Generates
  private Currency generateCurrency() {
    try {
      Method method = Currency.class.getMethod("getAvailableCurrencies");
      @SuppressWarnings("unchecked") // getAvailableCurrencies() returns Set<Currency>.
      Set<Currency> currencies = (Set<Currency>) method.invoke(null);
      return pickInstance(currencies, Currency.getInstance(Locale.US));
      /*
       * Do not merge the 2 catch blocks below. javac would infer a type of
       * ReflectiveOperationException, which Animal Sniffer would reject. (Old versions of
       * Android don't *seem* to mind, but there might be edge cases of which we're unaware.)
       */
    } catch (NoSuchMethodException notJava7) {
      return preJava7FreshCurrency();
    } catch (InvocationTargetException notJava7) {
      return preJava7FreshCurrency();
    } catch (IllegalAccessException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private Currency preJava7FreshCurrency() {
    for (Set<Locale> uselessLocales = Sets.newHashSet(); ; ) {
      Locale locale = generateLocale();
      if (uselessLocales.contains(locale)) { // exhausted all locales
        return Currency.getInstance(Locale.US);
      }
      try {
        return Currency.getInstance(locale);
      } catch (IllegalArgumentException e) {
        uselessLocales.add(locale);
      }
    }
  }

  @Empty
  private <T> Optional<T> generateJavaOptional() {
    return Optional.empty();
  }

  @Generates
  private <T> Optional<T> generateJavaOptional(T value) {
    return Optional.of(value);
  }

  @Generates
  private OptionalInt generateOptionalInt() {
    return OptionalInt.of(generateInt());
  }

  @Generates
  private OptionalLong generateOptionalLong() {
    return OptionalLong.of(generateLong());
  }

  @Generates
  private OptionalDouble generateOptionalDouble() {
    return OptionalDouble.of(generateDouble());
  }

  // common.base
  @Empty
  private <T> com.google.common.base.Optional<T> generateGoogleOptional() {
    return com.google.common.base.Optional.absent();
  }

  @Generates
  private <T> com.google.common.base.Optional<T> generateGoogleOptional(T value) {
    return com.google.common.base.Optional.of(value);
  }

  @Generates
  private Joiner generateJoiner() {
    return Joiner.on(generateString());
  }

  @Generates
  private Splitter generateSplitter() {
    return Splitter.on(generateString());
  }

  @Generates
  private <T> Equivalence<T> generateEquivalence() {
    return new Equivalence<T>() {
      @Override
      protected boolean doEquivalent(T a, T b) {
        return false;
      }

      @Override
      protected int doHash(T t) {
        return 0;
      }

      final String string = paramString(Equivalence.class, generateInt());

      @Override
      public String toString() {
        return string;
      }
    };
  }

  @Generates
  private CharMatcher generateCharMatcher() {
    return new CharMatcher() {
      @Override
      public boolean matches(char c) {
        return false;
      }

      final String string = paramString(CharMatcher.class, generateInt());

      @Override
      public String toString() {
        return string;
      }
    };
  }

  @Generates
  private Ticker generateTicker() {
    return new Ticker() {
      @Override
      public long read() {
        return 0;
      }

      final String string = paramString(Ticker.class, generateInt());

      @Override
      public String toString() {
        return string;
      }
    };
  }

  // collect
  @Generates
  private <T> Comparator<T> generateComparator() {
    return generateOrdering();
  }

  @Generates
  private <T> Ordering<T> generateOrdering() {
    return new Ordering<T>() {
      @Override
      public int compare(T left, T right) {
        return 0;
      }

      final String string = paramString(Ordering.class, generateInt());

      @Override
      public String toString() {
        return string;
      }
    };
  }

  @Empty
  private static <C extends Comparable<?>> Range<C> generateRange() {
    return Range.all();
  }

  @Generates
  private static <C extends Comparable<?>> Range<C> generateRange(C freshElement) {
    return Range.singleton(freshElement);
  }

  @Generates
  private static <E> Iterable<E> generateIterable(E freshElement) {
    return generateList(freshElement);
  }

  @Generates
  private static <E> Collection<E> generateCollection(E freshElement) {
    return generateList(freshElement);
  }

  @Generates
  private static <E> List<E> generateList(E freshElement) {
    return generateArrayList(freshElement);
  }

  @Generates
  private static <E> ArrayList<E> generateArrayList(E freshElement) {
    ArrayList<E> list = Lists.newArrayList();
    list.add(freshElement);
    return list;
  }

  @Generates
  private static <E> LinkedList<E> generateLinkedList(E freshElement) {
    LinkedList<E> list = Lists.newLinkedList();
    list.add(freshElement);
    return list;
  }

  @Generates
  private static <E> ImmutableList<E> generateImmutableList(E freshElement) {
    return ImmutableList.of(freshElement);
  }

  @Generates
  private static <E> ImmutableCollection<E> generateImmutableCollection(E freshElement) {
    return generateImmutableList(freshElement);
  }

  @Generates
  private static <E> Set<E> generateSet(E freshElement) {
    return generateHashSet(freshElement);
  }

  @Generates
  private static <E> HashSet<E> generateHashSet(E freshElement) {
    return generateLinkedHashSet(freshElement);
  }

  @Generates
  private static <E> LinkedHashSet<E> generateLinkedHashSet(E freshElement) {
    LinkedHashSet<E> set = Sets.newLinkedHashSet();
    set.add(freshElement);
    return set;
  }

  @Generates
  private static <E> ImmutableSet<E> generateImmutableSet(E freshElement) {
    return ImmutableSet.of(freshElement);
  }

  @Generates
  private static <E extends Comparable<? super E>> SortedSet<E> generateSortedSet(E freshElement) {
    return generateNavigableSet(freshElement);
  }

  @Generates
  private static <E extends Comparable<? super E>> NavigableSet<E> generateNavigableSet(
      E freshElement) {
    return generateTreeSet(freshElement);
  }

  @Generates
  private static <E extends Comparable<? super E>> TreeSet<E> generateTreeSet(E freshElement) {
    TreeSet<E> set = Sets.newTreeSet();
    set.add(freshElement);
    return set;
  }

  @Generates
  private static <E extends Comparable<? super E>> ImmutableSortedSet<E> generateImmutableSortedSet(
      E freshElement) {
    return ImmutableSortedSet.of(freshElement);
  }

  @Generates
  private static <E> Multiset<E> generateMultiset(E freshElement) {
    return generateHashMultiset(freshElement);
  }

  @Generates
  private static <E> HashMultiset<E> generateHashMultiset(E freshElement) {
    HashMultiset<E> multiset = HashMultiset.create();
    multiset.add(freshElement);
    return multiset;
  }

  @Generates
  private static <E> LinkedHashMultiset<E> generateLinkedHashMultiset(E freshElement) {
    LinkedHashMultiset<E> multiset = LinkedHashMultiset.create();
    multiset.add(freshElement);
    return multiset;
  }

  @Generates
  private static <E> ImmutableMultiset<E> generateImmutableMultiset(E freshElement) {
    return ImmutableMultiset.of(freshElement);
  }

  @Generates
  private static <E extends Comparable<E>> SortedMultiset<E> generateSortedMultiset(
      E freshElement) {
    return generateTreeMultiset(freshElement);
  }

  @Generates
  private static <E extends Comparable<E>> TreeMultiset<E> generateTreeMultiset(E freshElement) {
    TreeMultiset<E> multiset = TreeMultiset.create();
    multiset.add(freshElement);
    return multiset;
  }

  @Generates
  private static <E extends Comparable<E>>
      ImmutableSortedMultiset<E> generateImmutableSortedMultiset(E freshElement) {
    return ImmutableSortedMultiset.of(freshElement);
  }

  @Generates
  private static <K, V> Map<K, V> generateMap(K key, V value) {
    return generateHashdMap(key, value);
  }

  @Generates
  private static <K, V> HashMap<K, V> generateHashdMap(K key, V value) {
    return generateLinkedHashMap(key, value);
  }

  @Generates
  private static <K, V> LinkedHashMap<K, V> generateLinkedHashMap(K key, V value) {
    LinkedHashMap<K, V> map = Maps.newLinkedHashMap();
    map.put(key, value);
    return map;
  }

  @Generates
  private static <K, V> ImmutableMap<K, V> generateImmutableMap(K key, V value) {
    return ImmutableMap.of(key, value);
  }

  @Empty
  private static <K, V> ConcurrentMap<K, V> generateConcurrentMap() {
    return Maps.newConcurrentMap();
  }

  @Generates
  private static <K, V> ConcurrentMap<K, V> generateConcurrentMap(K key, V value) {
    ConcurrentMap<K, V> map = Maps.newConcurrentMap();
    map.put(key, value);
    return map;
  }

  @Generates
  private static <K extends Comparable<? super K>, V> SortedMap<K, V> generateSortedMap(
      K key, V value) {
    return generateNavigableMap(key, value);
  }

  @Generates
  private static <K extends Comparable<? super K>, V> NavigableMap<K, V> generateNavigableMap(
      K key, V value) {
    return generateTreeMap(key, value);
  }

  @Generates
  private static <K extends Comparable<? super K>, V> TreeMap<K, V> generateTreeMap(
      K key, V value) {
    TreeMap<K, V> map = Maps.newTreeMap();
    map.put(key, value);
    return map;
  }

  @Generates
  private static <K extends Comparable<? super K>, V>
      ImmutableSortedMap<K, V> generateImmutableSortedMap(K key, V value) {
    return ImmutableSortedMap.of(key, value);
  }

  @Generates
  private static <K, V> Multimap<K, V> generateMultimap(K key, V value) {
    return generateListMultimap(key, value);
  }

  @Generates
  private static <K, V> ImmutableMultimap<K, V> generateImmutableMultimap(K key, V value) {
    return ImmutableMultimap.of(key, value);
  }

  @Generates
  private static <K, V> ListMultimap<K, V> generateListMultimap(K key, V value) {
    return generateArrayListMultimap(key, value);
  }

  @Generates
  private static <K, V> ArrayListMultimap<K, V> generateArrayListMultimap(K key, V value) {
    ArrayListMultimap<K, V> multimap = ArrayListMultimap.create();
    multimap.put(key, value);
    return multimap;
  }

  @Generates
  private static <K, V> ImmutableListMultimap<K, V> generateImmutableListMultimap(K key, V value) {
    return ImmutableListMultimap.of(key, value);
  }

  @Generates
  private static <K, V> SetMultimap<K, V> generateSetMultimap(K key, V value) {
    return generateLinkedHashMultimap(key, value);
  }

  @Generates
  private static <K, V> HashMultimap<K, V> generateHashMultimap(K key, V value) {
    HashMultimap<K, V> multimap = HashMultimap.create();
    multimap.put(key, value);
    return multimap;
  }

  @Generates
  private static <K, V> LinkedHashMultimap<K, V> generateLinkedHashMultimap(K key, V value) {
    LinkedHashMultimap<K, V> multimap = LinkedHashMultimap.create();
    multimap.put(key, value);
    return multimap;
  }

  @Generates
  private static <K, V> ImmutableSetMultimap<K, V> generateImmutableSetMultimap(K key, V value) {
    return ImmutableSetMultimap.of(key, value);
  }

  @Generates
  private static <K, V> BiMap<K, V> generateBimap(K key, V value) {
    return generateHashBiMap(key, value);
  }

  @Generates
  private static <K, V> HashBiMap<K, V> generateHashBiMap(K key, V value) {
    HashBiMap<K, V> bimap = HashBiMap.create();
    bimap.put(key, value);
    return bimap;
  }

  @Generates
  private static <K, V> ImmutableBiMap<K, V> generateImmutableBimap(K key, V value) {
    return ImmutableBiMap.of(key, value);
  }

  @Generates
  private static <R, C, V> Table<R, C, V> generateTable(R row, C column, V value) {
    return generateHashBasedTable(row, column, value);
  }

  @Generates
  private static <R, C, V> HashBasedTable<R, C, V> generateHashBasedTable(
      R row, C column, V value) {
    HashBasedTable<R, C, V> table = HashBasedTable.create();
    table.put(row, column, value);
    return table;
  }

  @SuppressWarnings("rawtypes") // TreeBasedTable.create() is defined as such
  @Generates
  private static <R extends Comparable, C extends Comparable, V>
      RowSortedTable<R, C, V> generateRowSortedTable(R row, C column, V value) {
    return generateTreeBasedTable(row, column, value);
  }

  @SuppressWarnings("rawtypes") // TreeBasedTable.create() is defined as such
  @Generates
  private static <R extends Comparable, C extends Comparable, V>
      TreeBasedTable<R, C, V> generateTreeBasedTable(R row, C column, V value) {
    TreeBasedTable<R, C, V> table = TreeBasedTable.create();
    table.put(row, column, value);
    return table;
  }

  @Generates
  private static <R, C, V> ImmutableTable<R, C, V> generateImmutableTable(
      R row, C column, V value) {
    return ImmutableTable.of(row, column, value);
  }

  // common.reflect
  @Generates
  private TypeToken<?> generateTypeToken() {
    return TypeToken.of(generateClass());
  }

  // io types
  @Generates
  private File generateFile() {
    return new File(generateString());
  }

  @Generates
  private static ByteArrayInputStream generateByteArrayInputStream() {
    return new ByteArrayInputStream(new byte[0]);
  }

  @Generates
  private static InputStream generateInputStream() {
    return generateByteArrayInputStream();
  }

  @Generates
  private StringReader generateStringReader() {
    return new StringReader(generateString());
  }

  @Generates
  private Reader generateReader() {
    return generateStringReader();
  }

  @Generates
  private Readable generateReadable() {
    return generateReader();
  }

  @Generates
  private Buffer generateBuffer() {
    return generateCharBuffer();
  }

  @Generates
  private CharBuffer generateCharBuffer() {
    return CharBuffer.allocate(generateInt());
  }

  @Generates
  private ByteBuffer generateByteBuffer() {
    return ByteBuffer.allocate(generateInt());
  }

  @Generates
  private ShortBuffer generateShortBuffer() {
    return ShortBuffer.allocate(generateInt());
  }

  @Generates
  private IntBuffer generateIntBuffer() {
    return IntBuffer.allocate(generateInt());
  }

  @Generates
  private LongBuffer generateLongBuffer() {
    return LongBuffer.allocate(generateInt());
  }

  @Generates
  private FloatBuffer generateFloatBuffer() {
    return FloatBuffer.allocate(generateInt());
  }

  @Generates
  private DoubleBuffer generateDoubleBuffer() {
    return DoubleBuffer.allocate(generateInt());
  }
}
