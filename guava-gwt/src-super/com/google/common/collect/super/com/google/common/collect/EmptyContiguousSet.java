// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * An empty contiguous set.
 *
 * @author gak@google.com (Gregory Kick)
 */
@GwtCompatible(emulated = true)
@SuppressWarnings("unchecked") // allow ungenerified Comparable types
final class EmptyContiguousSet<C extends Comparable> extends ContiguousSet<C> {
  EmptyContiguousSet(DiscreteDomain<C> domain) {
    super(domain);
  }

  @Override public C first() {
    throw new NoSuchElementException();
  }

  @Override public C last() {
    throw new NoSuchElementException();
  }

  @Override public int size() {
    return 0;
  }

  @Override public ContiguousSet<C> intersection(ContiguousSet<C> other) {
    return this;
  }

  @Override public Range<C> range() {
    throw new NoSuchElementException();
  }

  @Override public Range<C> range(BoundType lowerBoundType, BoundType upperBoundType) {
    throw new NoSuchElementException();
  }

  @Override ContiguousSet<C> headSetImpl(C toElement, boolean inclusive) {
    return this;
  }

  @Override ContiguousSet<C> subSetImpl(C fromElement, boolean fromInclusive, C toElement,
      boolean toInclusive) {
    return this;
  }

  @Override ContiguousSet<C> tailSetImpl(C fromElement, boolean fromInclusive) {
    return this;
  }

  //Abstract method doesn't exist in GWT emulation
  /* @Override */ int indexOf(Object target) {
    return -1;
  }

  @Override public UnmodifiableIterator<C> iterator() {
    return Iterators.emptyIterator();
  }

  @Override boolean isPartialView() {
    return false;
  }

  @Override public boolean isEmpty() {
    return true;
  }

  @Override public ImmutableList<C> asList() {
    return ImmutableList.of();
  }

  @Override public String toString() {
    return "[]";
  }

  @Override public boolean equals(@Nullable Object object) {
    if (object instanceof Set) {
      Set<?> that = (Set<?>) object;
      return that.isEmpty();
    }
    return false;
  }

  @Override public int hashCode() {
    return 0;
  }
}

