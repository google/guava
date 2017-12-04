/*
 * Copyright (C) 2008 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.HashSet;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit tests for {@link Sets#union}, {@link Sets#intersection} and {@link Sets#difference}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class SetOperationsTest extends TestCase {
  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return Sets.union(Sets.<String>newHashSet(), Sets.<String>newHashSet());
                  }
                })
            .named("empty U empty")
            .withFeatures(
                CollectionSize.ZERO, CollectionFeature.NONE, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    checkArgument(elements.length == 1);
                    return Sets.union(Sets.<String>newHashSet(elements), Sets.newHashSet(elements));
                  }
                })
            .named("singleton U itself")
            .withFeatures(CollectionSize.ONE, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return Sets.union(Sets.<String>newHashSet(), Sets.newHashSet(elements));
                  }
                })
            .named("empty U set")
            .withFeatures(
                CollectionSize.ONE, CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return Sets.union(Sets.newHashSet(elements), Sets.<String>newHashSet());
                  }
                })
            .named("set U empty")
            .withFeatures(
                CollectionSize.ONE, CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    checkArgument(elements.length == 3);
                    // Put the sets in different orders for the hell of it
                    return Sets.union(
                        Sets.newLinkedHashSet(asList(elements)),
                        Sets.newLinkedHashSet(asList(elements[1], elements[0], elements[2])));
                  }
                })
            .named("set U itself")
            .withFeatures(CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    checkArgument(elements.length == 3);
                    return Sets.union(
                        Sets.newHashSet(elements[0]), Sets.newHashSet(elements[1], elements[2]));
                  }
                })
            .named("union of disjoint")
            .withFeatures(CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return Sets.union(
                        Sets.<String>newHashSet(elements[0], elements[1]),
                        Sets.newHashSet(elements[1], elements[2]));
                  }
                })
            .named("venn")
            .withFeatures(CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return Sets.intersection(Sets.<String>newHashSet(), Sets.<String>newHashSet());
                  }
                })
            .named("empty & empty")
            .withFeatures(
                CollectionSize.ZERO, CollectionFeature.NONE, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return Sets.intersection(
                        Sets.<String>newHashSet(), Sets.newHashSet((String) null));
                  }
                })
            .named("empty & singleton")
            .withFeatures(
                CollectionSize.ZERO, CollectionFeature.NONE, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return Sets.intersection(Sets.newHashSet("a", "b"), Sets.newHashSet("c", "d"));
                  }
                })
            .named("intersection of disjoint")
            .withFeatures(
                CollectionSize.ZERO, CollectionFeature.NONE, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return Sets.intersection(Sets.newHashSet(elements), Sets.newHashSet(elements));
                  }
                })
            .named("set & itself")
            .withFeatures(
                CollectionSize.ONE, CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return Sets.intersection(
                        Sets.newHashSet("a", elements[0], "b"),
                        Sets.newHashSet("c", elements[0], "d"));
                  }
                })
            .named("intersection with overlap of one")
            .withFeatures(CollectionSize.ONE, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return Sets.difference(Sets.<String>newHashSet(), Sets.<String>newHashSet());
                  }
                })
            .named("empty - empty")
            .withFeatures(
                CollectionSize.ZERO, CollectionFeature.NONE, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return Sets.difference(Sets.newHashSet("a"), Sets.newHashSet("a"));
                  }
                })
            .named("singleton - itself")
            .withFeatures(
                CollectionSize.ZERO, CollectionFeature.NONE, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Set<String> set = Sets.newHashSet("b", "c");
                    Set<String> other = Sets.newHashSet("a", "b", "c", "d");
                    return Sets.difference(set, other);
                  }
                })
            .named("set - superset")
            .withFeatures(
                CollectionSize.ZERO, CollectionFeature.NONE, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    Set<String> set = Sets.newHashSet(elements);
                    Set<String> other = Sets.newHashSet("wz", "xq");
                    set.addAll(other);
                    other.add("pq");
                    return Sets.difference(set, other);
                  }
                })
            .named("set - set")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return Sets.difference(Sets.newHashSet(elements), Sets.newHashSet());
                  }
                })
            .named("set - empty")
            .withFeatures(
                CollectionSize.ONE, CollectionSize.SEVERAL, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return Sets.difference(
                        Sets.<String>newHashSet(elements), Sets.newHashSet("xx", "xq"));
                  }
                })
            .named("set - disjoint")
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES)
            .createTestSuite());

    suite.addTestSuite(MoreTests.class);
    return suite;
  }

  public static class MoreTests extends TestCase {
    Set<String> friends;
    Set<String> enemies;

    @Override
    public void setUp() {
      friends = Sets.newHashSet("Tom", "Joe", "Dave");
      enemies = Sets.newHashSet("Dick", "Harry", "Tom");
    }

    public void testUnion() {
      Set<String> all = Sets.union(friends, enemies);
      assertEquals(5, all.size());

      ImmutableSet<String> immut = Sets.union(friends, enemies).immutableCopy();
      HashSet<String> mut = Sets.union(friends, enemies).copyInto(new HashSet<String>());

      enemies.add("Buck");
      assertEquals(6, all.size());
      assertEquals(5, immut.size());
      assertEquals(5, mut.size());
    }

    public void testIntersection() {
      Set<String> friends = Sets.newHashSet("Tom", "Joe", "Dave");
      Set<String> enemies = Sets.newHashSet("Dick", "Harry", "Tom");

      Set<String> frenemies = Sets.intersection(friends, enemies);
      assertEquals(1, frenemies.size());

      ImmutableSet<String> immut = Sets.intersection(friends, enemies).immutableCopy();
      HashSet<String> mut = Sets.intersection(friends, enemies).copyInto(new HashSet<String>());

      enemies.add("Joe");
      assertEquals(2, frenemies.size());
      assertEquals(1, immut.size());
      assertEquals(1, mut.size());
    }

    public void testDifference() {
      Set<String> friends = Sets.newHashSet("Tom", "Joe", "Dave");
      Set<String> enemies = Sets.newHashSet("Dick", "Harry", "Tom");

      Set<String> goodFriends = Sets.difference(friends, enemies);
      assertEquals(2, goodFriends.size());

      ImmutableSet<String> immut = Sets.difference(friends, enemies).immutableCopy();
      HashSet<String> mut = Sets.difference(friends, enemies).copyInto(new HashSet<String>());

      enemies.add("Dave");
      assertEquals(1, goodFriends.size());
      assertEquals(2, immut.size());
      assertEquals(2, mut.size());
    }

    public void testSymmetricDifference() {
      Set<String> friends = Sets.newHashSet("Tom", "Joe", "Dave");
      Set<String> enemies = Sets.newHashSet("Dick", "Harry", "Tom");

      Set<String> symmetricDifferenceFriendsFirst = Sets.symmetricDifference(friends, enemies);
      assertEquals(4, symmetricDifferenceFriendsFirst.size());

      Set<String> symmetricDifferenceEnemiesFirst = Sets.symmetricDifference(enemies, friends);
      assertEquals(4, symmetricDifferenceEnemiesFirst.size());

      assertEquals(symmetricDifferenceFriendsFirst, symmetricDifferenceEnemiesFirst);

      ImmutableSet<String> immut = Sets.symmetricDifference(friends, enemies).immutableCopy();
      HashSet<String> mut =
          Sets.symmetricDifference(friends, enemies).copyInto(new HashSet<String>());

      enemies.add("Dave");
      assertEquals(3, symmetricDifferenceFriendsFirst.size());
      assertEquals(4, immut.size());
      assertEquals(4, mut.size());

      immut = Sets.symmetricDifference(enemies, friends).immutableCopy();
      mut = Sets.symmetricDifference(enemies, friends).copyInto(new HashSet<String>());
      friends.add("Harry");
      assertEquals(2, symmetricDifferenceEnemiesFirst.size());
      assertEquals(3, immut.size());
      assertEquals(3, mut.size());
    }
  }
}
