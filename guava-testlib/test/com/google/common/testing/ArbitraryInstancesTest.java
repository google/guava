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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Equivalence;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.BiMap;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Range;
import com.google.common.collect.RowSortedTable;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.SortedMapDifference;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.Table;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.AtomicDouble;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
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
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Currency;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import junit.framework.TestCase;

/**
 * Unit test for {@link ArbitraryInstances}.
 *
 * @author Ben Yu
 */
public class ArbitraryInstancesTest extends TestCase {

  public void testGet_primitives() {
    assertNull(ArbitraryInstances.get(void.class));
    assertNull(ArbitraryInstances.get(Void.class));
    assertEquals(Boolean.FALSE, ArbitraryInstances.get(boolean.class));
    assertEquals(Boolean.FALSE, ArbitraryInstances.get(Boolean.class));
    assertEquals(Character.valueOf('\0'), ArbitraryInstances.get(char.class));
    assertEquals(Character.valueOf('\0'), ArbitraryInstances.get(Character.class));
    assertEquals(Byte.valueOf((byte) 0), ArbitraryInstances.get(byte.class));
    assertEquals(Byte.valueOf((byte) 0), ArbitraryInstances.get(Byte.class));
    assertEquals(Short.valueOf((short) 0), ArbitraryInstances.get(short.class));
    assertEquals(Short.valueOf((short) 0), ArbitraryInstances.get(Short.class));
    assertEquals(Integer.valueOf(0), ArbitraryInstances.get(int.class));
    assertEquals(Integer.valueOf(0), ArbitraryInstances.get(Integer.class));
    assertEquals(Long.valueOf(0), ArbitraryInstances.get(long.class));
    assertEquals(Long.valueOf(0), ArbitraryInstances.get(Long.class));
    assertEquals(Float.valueOf(0), ArbitraryInstances.get(float.class));
    assertEquals(Float.valueOf(0), ArbitraryInstances.get(Float.class));
    assertEquals(Double.valueOf(0), ArbitraryInstances.get(double.class));
    assertEquals(Double.valueOf(0), ArbitraryInstances.get(Double.class));
    assertEquals(UnsignedInteger.ZERO, ArbitraryInstances.get(UnsignedInteger.class));
    assertEquals(UnsignedLong.ZERO, ArbitraryInstances.get(UnsignedLong.class));
    assertEquals(0, ArbitraryInstances.get(BigDecimal.class).intValue());
    assertEquals(0, ArbitraryInstances.get(BigInteger.class).intValue());
    assertEquals("", ArbitraryInstances.get(String.class));
    assertEquals("", ArbitraryInstances.get(CharSequence.class));
    assertEquals(TimeUnit.SECONDS, ArbitraryInstances.get(TimeUnit.class));
    assertNotNull(ArbitraryInstances.get(Object.class));
    assertEquals(0, ArbitraryInstances.get(Number.class));
    assertEquals(Charsets.UTF_8, ArbitraryInstances.get(Charset.class));
    assertEquals(Optional.empty(), ArbitraryInstances.get(Optional.class));
    assertEquals(OptionalInt.empty(), ArbitraryInstances.get(OptionalInt.class));
    assertEquals(OptionalLong.empty(), ArbitraryInstances.get(OptionalLong.class));
    assertEquals(OptionalDouble.empty(), ArbitraryInstances.get(OptionalDouble.class));
    assertNotNull(ArbitraryInstances.get(UUID.class));
  }

  public void testGet_collections() {
    assertEquals(ImmutableSet.of().iterator(), ArbitraryInstances.get(Iterator.class));
    assertFalse(ArbitraryInstances.get(PeekingIterator.class).hasNext());
    assertFalse(ArbitraryInstances.get(ListIterator.class).hasNext());
    assertEquals(ImmutableSet.of(), ArbitraryInstances.get(Iterable.class));
    assertEquals(ImmutableSet.of(), ArbitraryInstances.get(Set.class));
    assertEquals(ImmutableSet.of(), ArbitraryInstances.get(ImmutableSet.class));
    assertEquals(ImmutableSortedSet.of(), ArbitraryInstances.get(SortedSet.class));
    assertEquals(ImmutableSortedSet.of(), ArbitraryInstances.get(ImmutableSortedSet.class));
    assertEquals(ImmutableList.of(), ArbitraryInstances.get(Collection.class));
    assertEquals(ImmutableList.of(), ArbitraryInstances.get(ImmutableCollection.class));
    assertEquals(ImmutableList.of(), ArbitraryInstances.get(List.class));
    assertEquals(ImmutableList.of(), ArbitraryInstances.get(ImmutableList.class));
    assertEquals(ImmutableMap.of(), ArbitraryInstances.get(Map.class));
    assertEquals(ImmutableMap.of(), ArbitraryInstances.get(ImmutableMap.class));
    assertEquals(ImmutableSortedMap.of(), ArbitraryInstances.get(SortedMap.class));
    assertEquals(ImmutableSortedMap.of(), ArbitraryInstances.get(ImmutableSortedMap.class));
    assertEquals(ImmutableMultiset.of(), ArbitraryInstances.get(Multiset.class));
    assertEquals(ImmutableMultiset.of(), ArbitraryInstances.get(ImmutableMultiset.class));
    assertTrue(ArbitraryInstances.get(SortedMultiset.class).isEmpty());
    assertEquals(ImmutableMultimap.of(), ArbitraryInstances.get(Multimap.class));
    assertEquals(ImmutableMultimap.of(), ArbitraryInstances.get(ImmutableMultimap.class));
    assertTrue(ArbitraryInstances.get(SortedSetMultimap.class).isEmpty());
    assertEquals(ImmutableTable.of(), ArbitraryInstances.get(Table.class));
    assertEquals(ImmutableTable.of(), ArbitraryInstances.get(ImmutableTable.class));
    assertTrue(ArbitraryInstances.get(RowSortedTable.class).isEmpty());
    assertEquals(ImmutableBiMap.of(), ArbitraryInstances.get(BiMap.class));
    assertEquals(ImmutableBiMap.of(), ArbitraryInstances.get(ImmutableBiMap.class));
    assertTrue(ArbitraryInstances.get(ImmutableClassToInstanceMap.class).isEmpty());
    assertTrue(ArbitraryInstances.get(ClassToInstanceMap.class).isEmpty());
    assertTrue(ArbitraryInstances.get(ListMultimap.class).isEmpty());
    assertTrue(ArbitraryInstances.get(ImmutableListMultimap.class).isEmpty());
    assertTrue(ArbitraryInstances.get(SetMultimap.class).isEmpty());
    assertTrue(ArbitraryInstances.get(ImmutableSetMultimap.class).isEmpty());
    assertTrue(ArbitraryInstances.get(MapDifference.class).areEqual());
    assertTrue(ArbitraryInstances.get(SortedMapDifference.class).areEqual());
    assertEquals(Range.all(), ArbitraryInstances.get(Range.class));
    assertTrue(ArbitraryInstances.get(NavigableSet.class).isEmpty());
    assertTrue(ArbitraryInstances.get(NavigableMap.class).isEmpty());
    assertTrue(ArbitraryInstances.get(LinkedList.class).isEmpty());
    assertTrue(ArbitraryInstances.get(Deque.class).isEmpty());
    assertTrue(ArbitraryInstances.get(Queue.class).isEmpty());
    assertTrue(ArbitraryInstances.get(PriorityQueue.class).isEmpty());
    assertTrue(ArbitraryInstances.get(BitSet.class).isEmpty());
    assertTrue(ArbitraryInstances.get(TreeSet.class).isEmpty());
    assertTrue(ArbitraryInstances.get(TreeMap.class).isEmpty());
    assertFreshInstanceReturned(
        LinkedList.class,
        Deque.class,
        Queue.class,
        PriorityQueue.class,
        BitSet.class,
        TreeSet.class,
        TreeMap.class);
  }

  public void testGet_misc() {
    assertNotNull(ArbitraryInstances.get(CharMatcher.class));
    assertNotNull(ArbitraryInstances.get(Currency.class).getCurrencyCode());
    assertNotNull(ArbitraryInstances.get(Locale.class));
    assertNotNull(ArbitraryInstances.get(Joiner.class).join(ImmutableList.of("a")));
    assertNotNull(ArbitraryInstances.get(Splitter.class).split("a,b"));
    assertThat(ArbitraryInstances.get(com.google.common.base.Optional.class)).isAbsent();
    ArbitraryInstances.get(Stopwatch.class).start();
    assertNotNull(ArbitraryInstances.get(Ticker.class));
    assertFreshInstanceReturned(Random.class);
    assertEquals(
        ArbitraryInstances.get(Random.class).nextInt(),
        ArbitraryInstances.get(Random.class).nextInt());
  }

  public void testGet_concurrent() {
    assertTrue(ArbitraryInstances.get(BlockingDeque.class).isEmpty());
    assertTrue(ArbitraryInstances.get(BlockingQueue.class).isEmpty());
    assertTrue(ArbitraryInstances.get(DelayQueue.class).isEmpty());
    assertTrue(ArbitraryInstances.get(SynchronousQueue.class).isEmpty());
    assertTrue(ArbitraryInstances.get(PriorityBlockingQueue.class).isEmpty());
    assertTrue(ArbitraryInstances.get(ConcurrentMap.class).isEmpty());
    assertTrue(ArbitraryInstances.get(ConcurrentNavigableMap.class).isEmpty());
    ArbitraryInstances.get(Executor.class).execute(ArbitraryInstances.get(Runnable.class));
    assertNotNull(ArbitraryInstances.get(ThreadFactory.class));
    assertFreshInstanceReturned(
        BlockingQueue.class,
        BlockingDeque.class,
        PriorityBlockingQueue.class,
        DelayQueue.class,
        SynchronousQueue.class,
        ConcurrentMap.class,
        ConcurrentNavigableMap.class,
        AtomicReference.class,
        AtomicBoolean.class,
        AtomicInteger.class,
        AtomicLong.class,
        AtomicDouble.class);
  }

  @SuppressWarnings("unchecked") // functor classes have no type parameters
  public void testGet_functors() {
    assertEquals(0, ArbitraryInstances.get(Comparator.class).compare("abc", 123));
    assertTrue(ArbitraryInstances.get(Predicate.class).apply("abc"));
    assertTrue(ArbitraryInstances.get(Equivalence.class).equivalent(1, 1));
    assertFalse(ArbitraryInstances.get(Equivalence.class).equivalent(1, 2));
  }

  @SuppressWarnings("SelfComparison")
  public void testGet_comparable() {
    @SuppressWarnings("unchecked") // The null value can compare with any Object
    Comparable<Object> comparable = ArbitraryInstances.get(Comparable.class);
    assertEquals(0, comparable.compareTo(comparable));
    assertTrue(comparable.compareTo("") > 0);
    try {
      comparable.compareTo(null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testGet_array() {
    assertThat(ArbitraryInstances.get(int[].class)).isEmpty();
    assertThat(ArbitraryInstances.get(Object[].class)).isEmpty();
    assertThat(ArbitraryInstances.get(String[].class)).isEmpty();
  }

  public void testGet_enum() {
    assertNull(ArbitraryInstances.get(EmptyEnum.class));
    assertEquals(Direction.UP, ArbitraryInstances.get(Direction.class));
  }

  public void testGet_interface() {
    assertNull(ArbitraryInstances.get(SomeInterface.class));
  }

  public void testGet_runnable() {
    ArbitraryInstances.get(Runnable.class).run();
  }

  public void testGet_class() {
    assertSame(SomeAbstractClass.INSTANCE, ArbitraryInstances.get(SomeAbstractClass.class));
    assertSame(
        WithPrivateConstructor.INSTANCE, ArbitraryInstances.get(WithPrivateConstructor.class));
    assertNull(ArbitraryInstances.get(NoDefaultConstructor.class));
    assertSame(
        WithExceptionalConstructor.INSTANCE,
        ArbitraryInstances.get(WithExceptionalConstructor.class));
    assertNull(ArbitraryInstances.get(NonPublicClass.class));
  }

  public void testGet_mutable() {
    assertEquals(0, ArbitraryInstances.get(ArrayList.class).size());
    assertEquals(0, ArbitraryInstances.get(HashMap.class).size());
    assertThat(ArbitraryInstances.get(Appendable.class).toString()).isEmpty();
    assertThat(ArbitraryInstances.get(StringBuilder.class).toString()).isEmpty();
    assertThat(ArbitraryInstances.get(StringBuffer.class).toString()).isEmpty();
    assertFreshInstanceReturned(
        ArrayList.class,
        HashMap.class,
        Appendable.class,
        StringBuilder.class,
        StringBuffer.class,
        Throwable.class,
        Exception.class);
  }

  public void testGet_io() throws IOException {
    assertEquals(-1, ArbitraryInstances.get(InputStream.class).read());
    assertEquals(-1, ArbitraryInstances.get(ByteArrayInputStream.class).read());
    assertEquals(-1, ArbitraryInstances.get(Readable.class).read(CharBuffer.allocate(1)));
    assertEquals(-1, ArbitraryInstances.get(Reader.class).read());
    assertEquals(-1, ArbitraryInstances.get(StringReader.class).read());
    assertEquals(0, ArbitraryInstances.get(Buffer.class).capacity());
    assertEquals(0, ArbitraryInstances.get(CharBuffer.class).capacity());
    assertEquals(0, ArbitraryInstances.get(ByteBuffer.class).capacity());
    assertEquals(0, ArbitraryInstances.get(ShortBuffer.class).capacity());
    assertEquals(0, ArbitraryInstances.get(IntBuffer.class).capacity());
    assertEquals(0, ArbitraryInstances.get(LongBuffer.class).capacity());
    assertEquals(0, ArbitraryInstances.get(FloatBuffer.class).capacity());
    assertEquals(0, ArbitraryInstances.get(DoubleBuffer.class).capacity());
    ArbitraryInstances.get(PrintStream.class).println("test");
    ArbitraryInstances.get(PrintWriter.class).println("test");
    assertNotNull(ArbitraryInstances.get(File.class));
    assertFreshInstanceReturned(
        ByteArrayOutputStream.class, OutputStream.class,
        Writer.class, StringWriter.class,
        PrintStream.class, PrintWriter.class);
    assertEquals(ByteSource.empty(), ArbitraryInstances.get(ByteSource.class));
    assertEquals(CharSource.empty(), ArbitraryInstances.get(CharSource.class));
    assertNotNull(ArbitraryInstances.get(ByteSink.class));
    assertNotNull(ArbitraryInstances.get(CharSink.class));
  }

  public void testGet_reflect() {
    assertNotNull(ArbitraryInstances.get(Type.class));
    assertNotNull(ArbitraryInstances.get(AnnotatedElement.class));
    assertNotNull(ArbitraryInstances.get(GenericDeclaration.class));
  }

  public void testGet_regex() {
    assertEquals(Pattern.compile("").pattern(), ArbitraryInstances.get(Pattern.class).pattern());
    assertEquals(0, ArbitraryInstances.get(MatchResult.class).groupCount());
  }

  public void testGet_usePublicConstant() {
    assertSame(WithPublicConstant.INSTANCE, ArbitraryInstances.get(WithPublicConstant.class));
  }

  public void testGet_useFirstPublicConstant() {
    assertSame(WithPublicConstants.FIRST, ArbitraryInstances.get(WithPublicConstants.class));
  }

  public void testGet_nullConstantIgnored() {
    assertSame(FirstConstantIsNull.SECOND, ArbitraryInstances.get(FirstConstantIsNull.class));
  }

  public void testGet_constantWithGenericsNotUsed() {
    assertNull(ArbitraryInstances.get(WithGenericConstant.class));
  }

  public void testGet_nullConstant() {
    assertNull(ArbitraryInstances.get(WithNullConstant.class));
  }

  public void testGet_constantTypeDoesNotMatch() {
    assertNull(ArbitraryInstances.get(ParentClassHasConstant.class));
  }

  public void testGet_nonPublicConstantNotUsed() {
    assertNull(ArbitraryInstances.get(NonPublicConstantIgnored.class));
  }

  public void testGet_nonStaticFieldNotUsed() {
    assertNull(ArbitraryInstances.get(NonStaticFieldIgnored.class));
  }

  public void testGet_constructorPreferredOverConstants() {
    assertNotNull(ArbitraryInstances.get(WithPublicConstructorAndConstant.class));
    assertTrue(
        ArbitraryInstances.get(WithPublicConstructorAndConstant.class)
            != ArbitraryInstances.get(WithPublicConstructorAndConstant.class));
  }

  public void testGet_nonFinalFieldNotUsed() {
    assertNull(ArbitraryInstances.get(NonFinalFieldIgnored.class));
  }

  private static void assertFreshInstanceReturned(Class<?>... mutableClasses) {
    for (Class<?> mutableClass : mutableClasses) {
      Object instance = ArbitraryInstances.get(mutableClass);
      assertNotNull("Expected to return non-null for: " + mutableClass, instance);
      assertNotSame(
          "Expected to return fresh instance for: " + mutableClass,
          instance,
          ArbitraryInstances.get(mutableClass));
    }
  }

  private enum EmptyEnum {}

  private enum Direction {
    UP,
    DOWN
  }

  public interface SomeInterface {}

  public abstract static class SomeAbstractClass {
    public static final SomeAbstractClass INSTANCE = new SomeAbstractClass() {};

    public SomeAbstractClass() {}
  }

  static class NonPublicClass {
    public NonPublicClass() {}
  }

  private static class WithPrivateConstructor {
    public static final WithPrivateConstructor INSTANCE = new WithPrivateConstructor();
  }

  public static class NoDefaultConstructor {
    public NoDefaultConstructor(@SuppressWarnings("unused") int i) {}
  }

  public static class WithExceptionalConstructor {
    public static final WithExceptionalConstructor INSTANCE =
        new WithExceptionalConstructor("whatever");

    public WithExceptionalConstructor() {
      throw new RuntimeException();
    }

    private WithExceptionalConstructor(String unused) {}
  }

  private static class WithPublicConstant {
    public static final WithPublicConstant INSTANCE = new WithPublicConstant();
  }

  private static class ParentClassHasConstant extends WithPublicConstant {}

  public static class WithGenericConstant<T> {
    public static final WithGenericConstant<String> STRING_CONSTANT = new WithGenericConstant<>();

    private WithGenericConstant() {}
  }

  public static class WithNullConstant {
    public static final WithNullConstant NULL = null;

    private WithNullConstant() {}
  }

  public static class WithPublicConstructorAndConstant {
    public static final WithPublicConstructorAndConstant INSTANCE =
        new WithPublicConstructorAndConstant();

    public WithPublicConstructorAndConstant() {}
  }

  private static class WithPublicConstants {
    public static final WithPublicConstants FIRST = new WithPublicConstants();

    // To test that we pick the first constant alphabetically
    @SuppressWarnings("unused")
    public static final WithPublicConstants SECOND = new WithPublicConstants();
  }

  private static class FirstConstantIsNull {
    // To test that null constant is ignored
    @SuppressWarnings("unused")
    public static final FirstConstantIsNull FIRST = null;

    public static final FirstConstantIsNull SECOND = new FirstConstantIsNull();
  }

  public static class NonFinalFieldIgnored {
    public static NonFinalFieldIgnored instance = new NonFinalFieldIgnored();

    private NonFinalFieldIgnored() {}
  }

  public static class NonPublicConstantIgnored {
    static final NonPublicConstantIgnored INSTANCE = new NonPublicConstantIgnored();

    private NonPublicConstantIgnored() {}
  }

  public static class NonStaticFieldIgnored {
    // This should cause infinite recursion. But it shouldn't be used anyway.
    public final NonStaticFieldIgnored instance = new NonStaticFieldIgnored();

    private NonStaticFieldIgnored() {}
  }
}
