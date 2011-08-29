/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.SerializableTester;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Unit test for {@link HashMultiset}.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class HashMultisetTest extends AbstractMultisetTest {
  @Override protected <E> Multiset<E> create() {
    return HashMultiset.create();
  }

  public void testCreate() {
    Multiset<String> multiset = HashMultiset.create();
    multiset.add("foo", 2);
    multiset.add("bar");
    assertEquals(3, multiset.size());
    assertEquals(2, multiset.count("foo"));
  }

  public void testCreateWithSize() {
    Multiset<String> multiset = HashMultiset.create(50);
    multiset.add("foo", 2);
    multiset.add("bar");
    assertEquals(3, multiset.size());
    assertEquals(2, multiset.count("foo"));
  }

  public void testCreateFromIterable() {
    Multiset<String> multiset
        = HashMultiset.create(Arrays.asList("foo", "bar", "foo"));
    assertEquals(3, multiset.size());
    assertEquals(2, multiset.count("foo"));
  }

  @GwtIncompatible("SerializableTester")
  public void testSerializationContainingSelf() {
    Multiset<Multiset<?>> multiset = HashMultiset.create();
    multiset.add(multiset, 2);
    Multiset<Multiset<?>> copy = SerializableTester.reserialize(multiset);
    assertEquals(2, copy.size());
    assertSame(copy, copy.iterator().next());
  }

  @GwtIncompatible("Only used by @GwtIncompatible code")
  private static class MultisetHolder implements Serializable {
    public Multiset<?> member;
    MultisetHolder(Multiset<?> multiset) {
      this.member = multiset;
    }
    private static final long serialVersionUID = 1L;
  }

  @GwtIncompatible("SerializableTester")
  public void testSerializationIndirectSelfReference() {
    Multiset<MultisetHolder> multiset = HashMultiset.create();
    MultisetHolder holder = new MultisetHolder(multiset);
    multiset.add(holder, 2);
    Multiset<MultisetHolder> copy = SerializableTester.reserialize(multiset);
    assertEquals(2, copy.size());
    assertSame(copy, copy.iterator().next().member);
  }

  /*
   * The behavior of toString() and iteration is tested by LinkedHashMultiset,
   * which shares a lot of code with HashMultiset and has deterministic
   * iteration order.
   */

  /**
   * This test fails with Java 6, preventing us from running
   * NullPointerTester on multisets.
  public void testAnnotations() throws Exception {
    Method method = HashMultiset.class.getDeclaredMethod(
        "add", Object.class, int.class);
    assertTrue(method.getParameterAnnotations()[0].length > 0);
  }
  */
  
  @Override
  @GwtIncompatible(
      "http://code.google.com/p/google-web-toolkit/issues/detail?id=3421")
  public void testEntryAfterRemove() {
    super.testEntryAfterRemove();
  }
  
  @Override
  @GwtIncompatible(
      "http://code.google.com/p/google-web-toolkit/issues/detail?id=3421")
  public void testEntryAfterClear() {
    super.testEntryAfterClear();
  }
  
  @Override
  @GwtIncompatible(
      "http://code.google.com/p/google-web-toolkit/issues/detail?id=3421")
  public void testEntryAfterEntrySetClear() {
    super.testEntryAfterEntrySetClear();
  }

  @Override
  @GwtIncompatible(
      "http://code.google.com/p/google-web-toolkit/issues/detail?id=3421")
  public void testEntryAfterEntrySetIteratorRemove() {
    super.testEntryAfterEntrySetIteratorRemove();
  }
  
  @Override
  @GwtIncompatible(
      "http://code.google.com/p/google-web-toolkit/issues/detail?id=3421")
  public void testEntryAfterElementSetIteratorRemove() {
    super.testEntryAfterElementSetIteratorRemove();
  }
}
