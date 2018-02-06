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

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.MultisetFeature;
import com.google.common.collect.testing.google.MultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;
import java.util.Arrays;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for {@link LinkedHashMultiset}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class LinkedHashMultisetTest extends TestCase {

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        MultisetTestSuiteBuilder.using(linkedHashMultisetGenerator())
            .named("LinkedHashMultiset")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.GENERAL_PURPOSE,
                MultisetFeature.ENTRIES_ARE_VIEWS)
            .createTestSuite());
    suite.addTestSuite(LinkedHashMultisetTest.class);
    return suite;
  }

  private static TestStringMultisetGenerator linkedHashMultisetGenerator() {
    return new TestStringMultisetGenerator() {
      @Override
      protected Multiset<String> create(String[] elements) {
        return LinkedHashMultiset.create(asList(elements));
      }

      @Override
      public List<String> order(List<String> insertionOrder) {
        List<String> order = Lists.newArrayList();
        for (String s : insertionOrder) {
          int index = order.indexOf(s);
          if (index == -1) {
            order.add(s);
          } else {
            order.add(index, s);
          }
        }
        return order;
      }
    };
  }

  public void testCreate() {
    Multiset<String> multiset = LinkedHashMultiset.create();
    multiset.add("foo", 2);
    multiset.add("bar");
    assertEquals(3, multiset.size());
    assertEquals(2, multiset.count("foo"));
    assertEquals("[foo x 2, bar]", multiset.toString());
  }

  public void testCreateWithSize() {
    Multiset<String> multiset = LinkedHashMultiset.create(50);
    multiset.add("foo", 2);
    multiset.add("bar");
    assertEquals(3, multiset.size());
    assertEquals(2, multiset.count("foo"));
    assertEquals("[foo x 2, bar]", multiset.toString());
  }

  public void testCreateFromIterable() {
    Multiset<String> multiset = LinkedHashMultiset.create(Arrays.asList("foo", "bar", "foo"));
    assertEquals(3, multiset.size());
    assertEquals(2, multiset.count("foo"));
    assertEquals("[foo x 2, bar]", multiset.toString());
  }

  public void testToString() {
    Multiset<String> ms = LinkedHashMultiset.create();
    ms.add("a", 3);
    ms.add("c", 1);
    ms.add("b", 2);

    assertEquals("[a x 3, c, b x 2]", ms.toString());
  }

  public void testLosesPlaceInLine() throws Exception {
    Multiset<String> ms = LinkedHashMultiset.create();
    ms.add("a");
    ms.add("b", 2);
    ms.add("c");
    assertThat(ms.elementSet()).containsExactly("a", "b", "c").inOrder();
    ms.remove("b");
    assertThat(ms.elementSet()).containsExactly("a", "b", "c").inOrder();
    ms.add("b");
    assertThat(ms.elementSet()).containsExactly("a", "b", "c").inOrder();
    ms.remove("b", 2);
    ms.add("b");
    assertThat(ms.elementSet()).containsExactly("a", "c", "b").inOrder();
  }
}
