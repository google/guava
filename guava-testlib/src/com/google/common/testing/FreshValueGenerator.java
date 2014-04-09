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

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Equivalence;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
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
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Generates fresh instances of types that are different from each other (if possible).
 *
 * @author Ben Yu
 */
class FreshValueGenerator {

  private static final ImmutableMap<Class<?>, Method> GENERATORS;
  static {
    ImmutableMap.Builder<Class<?>, Method> builder =
        ImmutableMap.builder();
    for (Method method : FreshValueGenerator.class.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Generates.class)) {
        builder.put(method.getReturnType(), method);
      }
    }
    GENERATORS = builder.build();
  }

  private final AtomicInteger differentiator = new AtomicInteger(1);
  private final ListMultimap<Class<?>, Object> sampleInstances = ArrayListMultimap.create();
  private final Set<Type> generatedOptionalTypes = Sets.newHashSet();

  <T> void addSampleInstances(Class<T> type, Iterable<? extends T> instances) {
    sampleInstances.putAll(checkNotNull(type), checkNotNull(instances));
  }

  /**
   * Returns a fresh instance for {@code type} if possible. The returned instance could be:
   * <ul>
   * <li>exactly of the given type, including generic type parameters, such as
   *     {@code ImmutableList<String>};
   * <li>of the raw type;
   * <li>null if no fresh value can be generated.
   * </ul>
   */
  @Nullable Object generate(TypeToken<?> type) {
    Class<?> rawType = type.getRawType();
    List<Object> samples = sampleInstances.get(rawType);
    Object sample = nextInstance(samples, null);
    if (sample != null) {
      return sample;
    }
    if (rawType.isEnum()) {
      return nextInstance(rawType.getEnumConstants(), null);
    }
    if (type.isArray()) {
      TypeToken<?> componentType = type.getComponentType();
      Object array = Array.newInstance(componentType.getRawType(), 1);
      Array.set(array, 0, generate(componentType));
      return array;
    }
    if (rawType == Optional.class && generatedOptionalTypes.add(type.getType())) {
      // For any Optional<T>, we'll first generate absent(). The next call generates a distinct
      // value of T to be wrapped in Optional.of().
      return Optional.absent();
    }
    Method generator = GENERATORS.get(rawType);
    if (generator != null) {
      ImmutableList<Parameter> params = Invokable.from(generator).getParameters();
      List<Object> args = Lists.newArrayListWithCapacity(params.size());
      TypeVariable<?>[] typeVars = rawType.getTypeParameters();
      for (int i = 0; i < params.size(); i++) {
        TypeToken<?> paramType = type.resolveType(typeVars[i]);
        // We require all @Generates methods to either be parameter-less or accept non-null
        // fresh values for their generic parameter types.
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
      try {
        return generator.invoke(this, args.toArray());
      } catch (InvocationTargetException e) {
        Throwables.propagate(e.getCause());
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
    return defaultGenerate(rawType);
  }

  @Nullable final <T> T generate(Class<T> type) {
    return Primitives.wrap(type).cast(generate(TypeToken.of(type)));
  }

  private <T> T defaultGenerate(Class<T> rawType) {
    if (rawType.isInterface()) {
      // always create a new proxy
      return newProxy(rawType);
    }
    return ArbitraryInstances.get(rawType);
  }

  final <T> T newProxy(final Class<T> interfaceType) {
    return Reflection.newProxy(interfaceType, new FreshInvocationHandler(interfaceType));
  }

  private final class FreshInvocationHandler extends AbstractInvocationHandler {
    private final int identity = freshInt();
    private final Class<?> interfaceType;

    FreshInvocationHandler(Class<?> interfaceType) {
      this.interfaceType = interfaceType;
    }

    @Override protected Object handleInvocation(Object proxy, Method method, Object[] args) {
      return interfaceMethodCalled(interfaceType, method);
    }

    @Override public int hashCode() {
      return identity;
    }

    @Override public boolean equals(@Nullable Object obj) {
      if (obj instanceof FreshInvocationHandler) {
        FreshInvocationHandler that = (FreshInvocationHandler) obj;
        return identity == that.identity;
      }
      return false;
    }

    @Override public String toString() {
      return paramString(interfaceType, identity);
    }
  }

  /** Subclasses can override to provide different return value for proxied interface methods. */
  Object interfaceMethodCalled(
      @SuppressWarnings("unused") Class<?> interfaceType,
      @SuppressWarnings("unused") Method method) {
    throw new UnsupportedOperationException();
  }

  private <T> T nextInstance(T[] instances, T defaultValue) {
    return nextInstance(Arrays.asList(instances), defaultValue);
  }

  private <T> T nextInstance(Collection<T> instances, T defaultValue) {
    if (instances.isEmpty()) {
      return defaultValue;
    }
    // freshInt() is 1-based.
    return Iterables.get(instances, (freshInt() - 1) % instances.size());
  }

  private static String paramString(Class<?> type, int i) {
    return type.getSimpleName() + '@' + i;
  }

  /**
   * Annotates a method to be the instance generator of a certain type. The return type is the
   * generated type. The method parameters are non-null fresh values for each method type variable
   * in the same type variable declaration order of the return type.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  private @interface Generates {}

  @Generates private Class<?> freshClass() {
    return nextInstance(
        ImmutableList.of(
            int.class, long.class, void.class,
            Object.class, Object[].class, Iterable.class),
        Object.class);
  }

  @Generates private Object freshObject() {
    return freshString();
  }

  @Generates private Number freshNumber() {
    return freshInt();
  }

  @Generates private int freshInt() {
    return differentiator.getAndIncrement();
  }

  @Generates private Integer freshInteger() {
    return new Integer(freshInt());
  }

  @Generates private long freshLong() {
    return freshInt();
  }

  @Generates private Long freshLongObject() {
    return new Long(freshLong());
  }

  @Generates private float freshFloat() {
    return freshInt();
  }

  @Generates private Float freshFloatObject() {
    return new Float(freshFloat());
  }

  @Generates private double freshDouble() {
    return freshInt();
  }

  @Generates private Double freshDoubleObject() {
    return new Double(freshDouble());
  }

  @Generates private short freshShort() {
    return (short) freshInt();
  }

  @Generates private Short freshShortObject() {
    return new Short(freshShort());
  }

  @Generates private byte freshByte() {
    return (byte) freshInt();
  }

  @Generates private Byte freshByteObject() {
    return new Byte(freshByte());
  }

  @Generates private char freshChar() {
    return freshString().charAt(0);
  }

  @Generates private Character freshCharacter() {
    return new Character(freshChar());
  }

  @Generates private boolean freshBoolean() {
    return freshInt() % 2 == 0;
  }

  @Generates private Boolean freshBooleanObject() {
    return new Boolean(freshBoolean());
  }

  @Generates private UnsignedInteger freshUnsignedInteger() {
    return UnsignedInteger.fromIntBits(freshInt());
  }

  @Generates private UnsignedLong freshUnsignedLong() {
    return UnsignedLong.fromLongBits(freshLong());
  }

  @Generates private BigInteger freshBigInteger() {
    return BigInteger.valueOf(freshInt());
  }

  @Generates private BigDecimal freshBigDecimal() {
    return BigDecimal.valueOf(freshInt());
  }

  @Generates private CharSequence freshCharSequence() {
    return freshString();
  }

  @Generates private String freshString() {
    return Integer.toString(freshInt());
  }

  @Generates private Comparable<?> freshComparable() {
    return freshString();
  }

  @Generates private Pattern freshPattern() {
    return Pattern.compile(freshString());
  }

  @Generates private Charset freshCharset() {
    return nextInstance(Charset.availableCharsets().values(), Charsets.UTF_8);
  }

  @Generates private Locale freshLocale() {
    return nextInstance(Locale.getAvailableLocales(), Locale.US);
  }

  @Generates private Currency freshCurrency() {
    for (Set<Locale> uselessLocales = Sets.newHashSet(); ; ) {
      Locale locale = freshLocale();
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

  // common.base
  @Generates private <T> Optional<T> freshOptional(T value) {
    return Optional.of(value);
  }

  @Generates private Joiner freshJoiner() {
    return Joiner.on(freshString());
  }

  @Generates private Splitter freshSplitter() {
    return Splitter.on(freshString());
  }

  @Generates private <T> Equivalence<T> freshEquivalence() {
    return new Equivalence<T>() {
      @Override protected boolean doEquivalent(T a, T b) {
        return false;
      }
      @Override protected int doHash(T t) {
        return 0;
      }
      final String string = paramString(Equivalence.class, freshInt());
      @Override public String toString() {
        return string;
      }
    };
  }

  @Generates private CharMatcher freshCharMatcher() {
    return new CharMatcher() {
      @Override public boolean matches(char c) {
        return false;
      }
      final String string = paramString(CharMatcher.class, freshInt());
      @Override public String toString() {
        return string;
      }
    };
  }

  @Generates private Ticker freshTicker() {
    return new Ticker() {
      @Override public long read() {
        return 0;
      }
      final String string = paramString(Ticker.class, freshInt());
      @Override public String toString() {
        return string;
      }
    };
  }

  // collect
  @Generates private <T> Comparator<T> freshComparator() {
    return freshOrdering();
  }

  @Generates private <T> Ordering<T> freshOrdering() {
    return new Ordering<T>() {
      @Override public int compare(T left, T right) {
        return 0;
      }
      final String string = paramString(Ordering.class, freshInt());
      @Override public String toString() {
        return string;
      }
    };
  }

  @Generates private static <C extends Comparable> Range<C> freshRange(C freshElement) {
    return Range.singleton(freshElement);
  }

  @Generates private static <E> Iterable<E> freshIterable(E freshElement) {
    return freshList(freshElement);
  }

  @Generates private static <E> Collection<E> freshCollection(E freshElement) {
    return freshList(freshElement);
  }

  @Generates private static <E> List<E> freshList(E freshElement) {
    return freshArrayList(freshElement);
  }

  @Generates private static <E> ArrayList<E> freshArrayList(E freshElement) {
    ArrayList<E> list = Lists.newArrayList();
    list.add(freshElement);
    return list;
  }

  @Generates private static <E> LinkedList<E> freshLinkedList(E freshElement) {
    LinkedList<E> list = Lists.newLinkedList();
    list.add(freshElement);
    return list;
  }

  @Generates private static <E> ImmutableList<E> freshImmutableList(E freshElement) {
    return ImmutableList.of(freshElement);
  }

  @Generates private static <E> ImmutableCollection<E> freshImmutableCollection(E freshElement) {
    return freshImmutableList(freshElement);
  }

  @Generates private static <E> Set<E> freshSet(E freshElement) {
    return freshHashSet(freshElement);
  }

  @Generates private static <E> HashSet<E> freshHashSet(E freshElement) {
    return freshLinkedHashSet(freshElement);
  }

  @Generates private static <E> LinkedHashSet<E> freshLinkedHashSet(E freshElement) {
    LinkedHashSet<E> set = Sets.newLinkedHashSet();
    set.add(freshElement);
    return set;
  }

  @Generates private static <E> ImmutableSet<E> freshImmutableSet(E freshElement) {
    return ImmutableSet.of(freshElement);
  }

  @Generates private static <E extends Comparable<? super E>> SortedSet<E>
      freshSortedSet(E freshElement) {
    return freshTreeSet(freshElement);
  }

  @Generates private static <E extends Comparable<? super E>> TreeSet<E> freshTreeSet(
      E freshElement) {
    TreeSet<E> set = Sets.newTreeSet();
    set.add(freshElement);
    return set;
  }

  @Generates private static <E extends Comparable<? super E>> ImmutableSortedSet<E>
      freshImmutableSortedSet(E freshElement) {
    return ImmutableSortedSet.of(freshElement);
  }

  @Generates private static <E> Multiset<E> freshMultiset(E freshElement) {
    return freshHashMultiset(freshElement);
  }

  @Generates private static <E> HashMultiset<E> freshHashMultiset(E freshElement) {
    HashMultiset<E> multiset = HashMultiset.create();
    multiset.add(freshElement);
    return multiset;
  }

  @Generates private static <E> LinkedHashMultiset<E> freshLinkedHashMultiset(E freshElement) {
    LinkedHashMultiset<E> multiset = LinkedHashMultiset.create();
    multiset.add(freshElement);
    return multiset;
  }

  @Generates private static <E> ImmutableMultiset<E> freshImmutableMultiset(E freshElement) {
    return ImmutableMultiset.of(freshElement);
  }

  @Generates private static <E extends Comparable<E>> SortedMultiset<E> freshSortedMultiset(
      E freshElement) {
    return freshTreeMultiset(freshElement);
  }

  @Generates private static <E extends Comparable<E>> TreeMultiset<E> freshTreeMultiset(
      E freshElement) {
    TreeMultiset<E> multiset = TreeMultiset.create();
    multiset.add(freshElement);
    return multiset;
  }

  @Generates private static <E extends Comparable<E>> ImmutableSortedMultiset<E>
      freshImmutableSortedMultiset(E freshElement) {
    return ImmutableSortedMultiset.of(freshElement);
  }

  @Generates private static <K, V> Map<K, V> freshMap(K key, V value) {
    return freshHashdMap(key, value);
  }

  @Generates private static <K, V> HashMap<K, V> freshHashdMap(K key, V value) {
    return freshLinkedHashMap(key, value);
  }

  @Generates private static <K, V> LinkedHashMap<K, V> freshLinkedHashMap(K key, V value) {
    LinkedHashMap<K, V> map = Maps.newLinkedHashMap();
    map.put(key, value);
    return map;
  }

  @Generates private static <K, V> ImmutableMap<K, V> freshImmutableMap(K key, V value) {
    return ImmutableMap.of(key, value);
  }

  @Generates private static <K, V> ConcurrentMap<K, V> freshConcurrentMap(K key, V value) {
    ConcurrentMap<K, V> map = Maps.newConcurrentMap();
    map.put(key, value);
    return map;
  }

  @Generates private static <K extends Comparable<? super K>, V> SortedMap<K, V>
      freshSortedMap(K key, V value) {
    return freshTreeMap(key, value);
  }

  @Generates private static <K extends Comparable<? super K>, V> TreeMap<K, V> freshTreeMap(
      K key, V value) {
    TreeMap<K, V> map = Maps.newTreeMap();
    map.put(key, value);
    return map;
  }

  @Generates private static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V>
      freshImmutableSortedMap(K key, V value) {
    return ImmutableSortedMap.of(key, value);
  }

  @Generates private static <K, V> Multimap<K, V> freshMultimap(K key, V value) {
    return freshListMultimap(key, value);
  }

  @Generates private static <K, V> ImmutableMultimap<K, V> freshImmutableMultimap(K key, V value) {
    return ImmutableMultimap.of(key, value);
  }

  @Generates private static <K, V> ListMultimap<K, V> freshListMultimap(K key, V value) {
    return freshArrayListMultimap(key, value);
  }

  @Generates private static <K, V> ArrayListMultimap<K, V> freshArrayListMultimap(K key, V value) {
    ArrayListMultimap<K, V> multimap = ArrayListMultimap.create();
    multimap.put(key, value);
    return multimap;
  }

  @Generates private static <K, V> ImmutableListMultimap<K, V> freshImmutableListMultimap(
      K key, V value) {
    return ImmutableListMultimap.of(key, value);
  }

  @Generates private static <K, V> SetMultimap<K, V> freshSetMultimap(K key, V value) {
    return freshLinkedHashMultimap(key, value);
  }

  @Generates private static <K, V> HashMultimap<K, V> freshHashMultimap(K key, V value) {
    HashMultimap<K, V> multimap = HashMultimap.create();
    multimap.put(key, value);
    return multimap;
  }

  @Generates private static <K, V> LinkedHashMultimap<K, V> freshLinkedHashMultimap(
      K key, V value) {
    LinkedHashMultimap<K, V> multimap = LinkedHashMultimap.create();
    multimap.put(key, value);
    return multimap;
  }

  @Generates private static <K, V> ImmutableSetMultimap<K, V> freshImmutableSetMultimap(
      K key, V value) {
    return ImmutableSetMultimap.of(key, value);
  }

  @Generates private static <K, V> BiMap<K, V> freshBimap(K key, V value) {
    return freshHashBiMap(key, value);
  }

  @Generates private static <K, V> HashBiMap<K, V> freshHashBiMap(K key, V value) {
    HashBiMap<K, V> bimap = HashBiMap.create();
    bimap.put(key, value);
    return bimap;
  }

  @Generates private static <K, V> ImmutableBiMap<K, V> freshImmutableBimap(
      K key, V value) {
    return ImmutableBiMap.of(key, value);
  }

  @Generates private static <R, C, V> Table<R, C, V> freshTable(R row, C column, V value) {
    return freshHashBasedTable(row, column, value);
  }

  @Generates private static <R, C, V> HashBasedTable<R, C, V> freshHashBasedTable(
      R row, C column, V value) {
    HashBasedTable<R, C, V> table = HashBasedTable.create();
    table.put(row, column, value);
    return table;
  }

  @SuppressWarnings("rawtypes") // TreeBasedTable.create() is defined as such
  @Generates private static <R extends Comparable, C extends Comparable, V> RowSortedTable<R, C, V>
      freshRowSortedTable(R row, C column, V value) {
    return freshTreeBasedTable(row, column, value);
  }

  @SuppressWarnings("rawtypes") // TreeBasedTable.create() is defined as such
  @Generates private static <R extends Comparable, C extends Comparable, V> TreeBasedTable<R, C, V>
      freshTreeBasedTable(R row, C column, V value) {
    TreeBasedTable<R, C, V> table = TreeBasedTable.create();
    table.put(row, column, value);
    return table;
  }

  @Generates private static <R, C, V> ImmutableTable<R, C, V> freshImmutableTable(
      R row, C column, V value) {
    return ImmutableTable.of(row, column, value);
  }

  // common.reflect
  @Generates private TypeToken<?> freshTypeToken() {
    return TypeToken.of(freshClass());
  }

  // io types
  @Generates private File freshFile() {
    return new File(freshString());
  }

  @Generates private static ByteArrayInputStream freshByteArrayInputStream() {
    return new ByteArrayInputStream(new byte[0]);
  }

  @Generates private static InputStream freshInputStream() {
    return freshByteArrayInputStream();
  }

  @Generates private StringReader freshStringReader() {
    return new StringReader(freshString());
  }

  @Generates private Reader freshReader() {
    return freshStringReader();
  }

  @Generates private Readable freshReadable() {
    return freshReader();
  }

  @Generates private Buffer freshBuffer() {
    return freshCharBuffer();
  }

  @Generates private CharBuffer freshCharBuffer() {
    return CharBuffer.allocate(freshInt());
  }

  @Generates private ByteBuffer freshByteBuffer() {
    return ByteBuffer.allocate(freshInt());
  }

  @Generates private ShortBuffer freshShortBuffer() {
    return ShortBuffer.allocate(freshInt());
  }

  @Generates private IntBuffer freshIntBuffer() {
    return IntBuffer.allocate(freshInt());
  }

  @Generates private LongBuffer freshLongBuffer() {
    return LongBuffer.allocate(freshInt());
  }

  @Generates private FloatBuffer freshFloatBuffer() {
    return FloatBuffer.allocate(freshInt());
  }

  @Generates private DoubleBuffer freshDoubleBuffer() {
    return DoubleBuffer.allocate(freshInt());
  }
}
