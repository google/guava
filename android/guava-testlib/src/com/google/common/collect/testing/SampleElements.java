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

package com.google.common.collect.testing;

import com.google.common.annotations.GwtCompatible;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * A container class for the five sample elements we need for testing.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible
public class SampleElements<E> implements Iterable<E> {
  // TODO: rename e3, e4 => missing1, missing2
  private final E e0;
  private final E e1;
  private final E e2;
  private final E e3;
  private final E e4;

  public SampleElements(E e0, E e1, E e2, E e3, E e4) {
    this.e0 = e0;
    this.e1 = e1;
    this.e2 = e2;
    this.e3 = e3;
    this.e4 = e4;
  }

  @Override
  public Iterator<E> iterator() {
    return asList().iterator();
  }

  public List<E> asList() {
    return Arrays.asList(e0(), e1(), e2(), e3(), e4());
  }

  public static class Strings extends SampleElements<String> {
    public Strings() {
      // elements aren't sorted, to better test SortedSet iteration ordering
      super("b", "a", "c", "d", "e");
    }

    // for testing SortedSet and SortedMap methods
    public static final String BEFORE_FIRST = "\0";
    public static final String BEFORE_FIRST_2 = "\0\0";
    public static final String MIN_ELEMENT = "a";
    public static final String AFTER_LAST = "z";
    public static final String AFTER_LAST_2 = "zz";
  }

  public static class Chars extends SampleElements<Character> {
    public Chars() {
      // elements aren't sorted, to better test SortedSet iteration ordering
      super('b', 'a', 'c', 'd', 'e');
    }
  }

  public static class Enums extends SampleElements<AnEnum> {
    public Enums() {
      // elements aren't sorted, to better test SortedSet iteration ordering
      super(AnEnum.B, AnEnum.A, AnEnum.C, AnEnum.D, AnEnum.E);
    }
  }

  public static class Ints extends SampleElements<Integer> {
    public Ints() {
      // elements aren't sorted, to better test SortedSet iteration ordering
      super(1, 0, 2, 3, 4);
    }
  }

  public static <K, V> SampleElements<Entry<K, V>> mapEntries(
      SampleElements<K> keys, SampleElements<V> values) {
    return new SampleElements<>(
        Helpers.mapEntry(keys.e0(), values.e0()),
        Helpers.mapEntry(keys.e1(), values.e1()),
        Helpers.mapEntry(keys.e2(), values.e2()),
        Helpers.mapEntry(keys.e3(), values.e3()),
        Helpers.mapEntry(keys.e4(), values.e4()));
  }

  public E e0() {
    return e0;
  }

  public E e1() {
    return e1;
  }

  public E e2() {
    return e2;
  }

  public E e3() {
    return e3;
  }

  public E e4() {
    return e4;
  }

  public static class Unhashables extends SampleElements<UnhashableObject> {
    public Unhashables() {
      super(
          new UnhashableObject(1),
          new UnhashableObject(2),
          new UnhashableObject(3),
          new UnhashableObject(4),
          new UnhashableObject(5));
    }
  }

  public static class Colliders extends SampleElements<Object> {
    public Colliders() {
      super(new Collider(1), new Collider(2), new Collider(3), new Collider(4), new Collider(5));
    }
  }

  private static class Collider {
    final int value;

    Collider(int value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Collider && ((Collider) obj).value == value;
    }

    @Override
    public int hashCode() {
      return 1; // evil!
    }
  }
}
