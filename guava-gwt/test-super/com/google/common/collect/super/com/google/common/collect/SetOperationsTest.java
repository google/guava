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

import com.google.common.annotations.GwtCompatible;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for {@link Sets#union}, {@link Sets#intersection} and
 * {@link Sets#difference}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class SetOperationsTest extends TestCase {

  public static class MoreTests extends TestCase {
    Set<String> friends;
    Set<String> enemies;

    @Override public void setUp() {
      friends = Sets.newHashSet("Tom", "Joe", "Dave");
      enemies = Sets.newHashSet("Dick", "Harry", "Tom");
    }

    public void testUnion() {
      Set<String> all = Sets.union(friends, enemies);
      assertEquals(5, all.size());

      ImmutableSet<String> immut = Sets.union(friends, enemies).immutableCopy();
      HashSet<String> mut
          = Sets.union(friends, enemies).copyInto(new HashSet<String>());

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

      ImmutableSet<String> immut
          = Sets.intersection(friends, enemies).immutableCopy();
      HashSet<String> mut
          = Sets.intersection(friends, enemies).copyInto(new HashSet<String>());

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

      ImmutableSet<String> immut
          = Sets.difference(friends, enemies).immutableCopy();
      HashSet<String> mut
          = Sets.difference(friends, enemies).copyInto(new HashSet<String>());

      enemies.add("Dave");
      assertEquals(1, goodFriends.size());
      assertEquals(2, immut.size());
      assertEquals(2, mut.size());
    }

    public void testSymmetricDifference() {
      Set<String> friends = Sets.newHashSet("Tom", "Joe", "Dave");
      Set<String> enemies = Sets.newHashSet("Dick", "Harry", "Tom");

      Set<String> symmetricDifferenceFriendsFirst = Sets.symmetricDifference(
          friends, enemies);
      assertEquals(4, symmetricDifferenceFriendsFirst.size());

      Set<String> symmetricDifferenceEnemiesFirst = Sets.symmetricDifference(
          enemies, friends);
      assertEquals(4, symmetricDifferenceEnemiesFirst.size());

      assertEquals(symmetricDifferenceFriendsFirst,
          symmetricDifferenceEnemiesFirst);

      ImmutableSet<String> immut
          = Sets.symmetricDifference(friends, enemies).immutableCopy();
      HashSet<String> mut = Sets.symmetricDifference(friends, enemies)
          .copyInto(new HashSet<String>());

      enemies.add("Dave");
      assertEquals(3, symmetricDifferenceFriendsFirst.size());
      assertEquals(4, immut.size());
      assertEquals(4, mut.size());

      immut = Sets.symmetricDifference(enemies, friends).immutableCopy();
      mut = Sets.symmetricDifference(enemies, friends).
          copyInto(new HashSet<String>());
      friends.add("Harry");
      assertEquals(2, symmetricDifferenceEnemiesFirst.size());
      assertEquals(3, immut.size());
      assertEquals(3, mut.size());
    }
  }
}

