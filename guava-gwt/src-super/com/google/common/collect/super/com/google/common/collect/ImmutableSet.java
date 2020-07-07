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

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;

/**
 * GWT emulated version of {@link com.google.common.collect.ImmutableSet}. For the unsorted sets,
 * they are thin wrapper around {@link java.util.Collections#emptySet()}, {@link
 * Collections#singleton(Object)} and {@link java.util.LinkedHashSet} for empty, singleton and
 * regular sets respectively. For the sorted sets, it's a thin wrapper around {@link
 * java.util.TreeSet}.
 *
 * @see ImmutableSortedSet
 * @author Hayward Chan
 */
@SuppressWarnings("serial") // Serialization only done in GWT.
public abstract class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {
  ImmutableSet() {}

  @Beta
  public static <E> Collector<E, ?, ImmutableSet<E>> toImmutableSet() {
    return CollectCollectors.toImmutableSet();
  }

  // Casting to any type is safe because the set will never hold any elements.
  @SuppressWarnings({"unchecked"})
  public static <E> ImmutableSet<E> of() {
    return (ImmutableSet<E>) RegularImmutableSet.EMPTY;
  }

  public static <E> ImmutableSet<E> of(E element) {
    return new SingletonImmutableSet<E>(element);
  }

  @SuppressWarnings("unchecked")
  public static <E> ImmutableSet<E> of(E e1, E e2) {
    return create(e1, e2);
  }

  @SuppressWarnings("unchecked")
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3) {
    return create(e1, e2, e3);
  }

  @SuppressWarnings("unchecked")
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4) {
    return create(e1, e2, e3, e4);
  }

  @SuppressWarnings("unchecked")
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5) {
    return create(e1, e2, e3, e4, e5);
  }

  @SuppressWarnings("unchecked")
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E... others) {
    int size = others.length + 6;
    List<E> all = new ArrayList<E>(size);
    Collections.addAll(all, e1, e2, e3, e4, e5, e6);
    Collections.addAll(all, others);
    return copyOf(all.iterator());
  }

  public static <E> ImmutableSet<E> copyOf(E[] elements) {
    checkNotNull(elements);
    switch (elements.length) {
      case 0:
        return of();
      case 1:
        return of(elements[0]);
      default:
        return create(elements);
    }
  }

  public static <E> ImmutableSet<E> copyOf(Collection<? extends E> elements) {
    Iterable<? extends E> iterable = elements;
    return copyOf(iterable);
  }

  public static <E> ImmutableSet<E> copyOf(Iterable<? extends E> elements) {
    if (elements instanceof ImmutableSet && !(elements instanceof ImmutableSortedSet)) {
      @SuppressWarnings("unchecked") // all supported methods are covariant
      ImmutableSet<E> set = (ImmutableSet<E>) elements;
      return set;
    }
    return copyOf(elements.iterator());
  }

  public static <E> ImmutableSet<E> copyOf(Iterator<? extends E> elements) {
    if (!elements.hasNext()) {
      return of();
    }
    E first = elements.next();
    if (!elements.hasNext()) {
      // TODO: Remove "ImmutableSet.<E>" when eclipse bug is fixed.
      return ImmutableSet.<E>of(first);
    }

    Set<E> delegate = Sets.newLinkedHashSet();
    delegate.add(checkNotNull(first));
    do {
      delegate.add(checkNotNull(elements.next()));
    } while (elements.hasNext());

    return unsafeDelegate(delegate);
  }

  // Factory methods that skips the null checks on elements, only used when
  // the elements are known to be non-null.
  static <E> ImmutableSet<E> unsafeDelegate(Set<E> delegate) {
    switch (delegate.size()) {
      case 0:
        return of();
      case 1:
        return new SingletonImmutableSet<E>(delegate.iterator().next());
      default:
        return new RegularImmutableSet<E>(delegate);
    }
  }

  private static <E> ImmutableSet<E> create(E... elements) {
    // Create the set first, to remove duplicates if necessary.
    Set<E> set = Sets.newLinkedHashSet();
    Collections.addAll(set, elements);
    for (E element : set) {
      checkNotNull(element);
    }

    switch (set.size()) {
      case 0:
        return of();
      case 1:
        return new SingletonImmutableSet<E>(set.iterator().next());
      default:
        return new RegularImmutableSet<E>(set);
    }
  }

  @Override
  public boolean equals(Object obj) {
    return Sets.equalsImpl(this, obj);
  }

  @Override
  public int hashCode() {
    return Sets.hashCodeImpl(this);
  }

  // This declaration is needed to make Set.iterator() and
  // ImmutableCollection.iterator() appear consistent to javac's type inference.
  @Override
  public abstract UnmodifiableIterator<E> iterator();

  abstract static class Indexed<E> extends ImmutableSet<E> {
    abstract E get(int index);

    @Override
    public UnmodifiableIterator<E> iterator() {
      return asList().iterator();
    }

    @Override
    ImmutableList<E> createAsList() {
      return new ImmutableAsList<E>() {
        @Override
        public E get(int index) {
          return Indexed.this.get(index);
        }

        @Override
        Indexed<E> delegateCollection() {
          return Indexed.this;
        }
      };
    }
  }

  public static <E> Builder<E> builder() {
    return new Builder<E>();
  }

  public static <E> Builder<E> builderWithExpectedSize(int size) {
    return new Builder<E>(size);
  }

  public static class Builder<E> extends ImmutableCollection.Builder<E> {
    // accessed directly by ImmutableSortedSet
    final ArrayList<E> contents;

    public Builder() {
      this.contents = Lists.newArrayList();
    }

    Builder(int initialCapacity) {
      this.contents = Lists.newArrayListWithCapacity(initialCapacity);
    }

    @Override
    public Builder<E> add(E element) {
      contents.add(checkNotNull(element));
      return this;
    }

    @Override
    public Builder<E> add(E... elements) {
      checkNotNull(elements); // for GWT
      contents.ensureCapacity(contents.size() + elements.length);
      super.add(elements);
      return this;
    }

    @Override
    public Builder<E> addAll(Iterable<? extends E> elements) {
      if (elements instanceof Collection) {
        Collection<?> collection = (Collection<?>) elements;
        contents.ensureCapacity(contents.size() + collection.size());
      }
      super.addAll(elements);
      return this;
    }

    @Override
    public Builder<E> addAll(Iterator<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    Builder<E> combine(Builder<E> builder) {
      contents.addAll(builder.contents);
      return this;
    }

    @Override
    public ImmutableSet<E> build() {
      return copyOf(contents.iterator());
    }
  }
}
