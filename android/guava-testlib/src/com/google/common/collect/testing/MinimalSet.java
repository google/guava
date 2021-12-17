/*
 * Copyright (C) 2009 The Guava Authors
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A simplistic set which implements the bare minimum so that it can be used in tests without
 * relying on any specific Set implementations. Slow. Explicitly allows null elements so that they
 * can be used in the testers.
 *
 * @author Regina O'Dell
 */
@GwtCompatible
public class MinimalSet<E> extends MinimalCollection<E> implements Set<E> {

  @SuppressWarnings("unchecked") // empty Object[] as E[]
  public static <E> MinimalSet<E> of(E... contents) {
    return ofClassAndContents(Object.class, (E[]) new Object[0], Arrays.asList(contents));
  }

  @SuppressWarnings("unchecked") // empty Object[] as E[]
  public static <E> MinimalSet<E> from(Collection<? extends E> contents) {
    return ofClassAndContents(Object.class, (E[]) new Object[0], contents);
  }

  public static <E> MinimalSet<E> ofClassAndContents(
      Class<? super E> type, E[] emptyArrayForContents, Iterable<? extends E> contents) {
    List<E> setContents = new ArrayList<>();
    for (E e : contents) {
      if (!setContents.contains(e)) {
        setContents.add(e);
      }
    }
    return new MinimalSet<>(type, setContents.toArray(emptyArrayForContents));
  }

  private MinimalSet(Class<? super E> type, E... contents) {
    super(type, true, contents);
  }

  /*
   * equals() and hashCode() are more specific in the Set contract.
   */

  @Override
  public boolean equals(Object object) {
    if (object instanceof Set) {
      Set<?> that = (Set<?>) object;
      return (this.size() == that.size()) && this.containsAll(that);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hashCodeSum = 0;
    for (Object o : this) {
      hashCodeSum += (o == null) ? 0 : o.hashCode();
    }
    return hashCodeSum;
  }
}
