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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A multiset which forwards all its method calls to another multiset. Subclasses should override
 * one or more methods to modify the behavior of the backing multiset as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>Warning:</b> The methods of {@code ForwardingMultiset} forward <b>indiscriminately</b> to
 * the methods of the delegate. For example, overriding {@link #add(Object, int)} alone <b>will
 * not</b> change the behavior of {@link #add(Object)}, which can lead to unexpected behavior. In
 * this case, you should override {@code add(Object)} as well, either providing your own
 * implementation, or delegating to the provided {@code standardAdd} method.
 *
 * <p><b>{@code default} method warning:</b> This class does <i>not</i> forward calls to {@code
 * default} methods. Instead, it inherits their default implementations. When those implementations
 * invoke methods, they invoke methods on the {@code ForwardingMultiset}.
 *
 * <p>The {@code standard} methods and any collection views they return are not guaranteed to be
 * thread-safe, even when all of the methods that they depend on are thread-safe.
 *
 * @author Kevin Bourrillion
 * @author Louis Wasserman
 * @since 2.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public abstract class ForwardingMultiset<E extends @Nullable Object> extends ForwardingCollection<E>
    implements Multiset<E> {

  /** Constructor for use by subclasses. */
  protected ForwardingMultiset() {}

  @Override
  protected abstract Multiset<E> delegate();

  @Override
  public int count(@CheckForNull Object element) {
    return delegate().count(element);
  }

  @CanIgnoreReturnValue
  @Override
  public int add(@ParametricNullness E element, int occurrences) {
    return delegate().add(element, occurrences);
  }

  @CanIgnoreReturnValue
  @Override
  public int remove(@CheckForNull Object element, int occurrences) {
    return delegate().remove(element, occurrences);
  }

  @Override
  public Set<E> elementSet() {
    return delegate().elementSet();
  }

  @Override
  public Set<Entry<E>> entrySet() {
    return delegate().entrySet();
  }

  @Override
  public boolean equals(@CheckForNull Object object) {
    return object == this || delegate().equals(object);
  }

  @Override
  public int hashCode() {
    return delegate().hashCode();
  }

  @CanIgnoreReturnValue
  @Override
  public int setCount(@ParametricNullness E element, int count) {
    return delegate().setCount(element, count);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean setCount(@ParametricNullness E element, int oldCount, int newCount) {
    return delegate().setCount(element, oldCount, newCount);
  }

  /**
   * A sensible definition of {@link #contains} in terms of {@link #count}. If you override {@link
   * #count}, you may wish to override {@link #contains} to forward to this implementation.
   *
   * @since 7.0
   */
  @Override
  protected boolean standardContains(@CheckForNull Object object) {
    return count(object) > 0;
  }

  /**
   * A sensible definition of {@link #clear} in terms of the {@code iterator} method of {@link
   * #entrySet}. If you override {@link #entrySet}, you may wish to override {@link #clear} to
   * forward to this implementation.
   *
   * @since 7.0
   */
  @Override
  protected void standardClear() {
    Iterators.clear(entrySet().iterator());
  }

  /**
   * A sensible, albeit inefficient, definition of {@link #count} in terms of {@link #entrySet}. If
   * you override {@link #entrySet}, you may wish to override {@link #count} to forward to this
   * implementation.
   *
   * @since 7.0
   */
  @Beta
  protected int standardCount(@CheckForNull Object object) {
    for (Entry<?> entry : this.entrySet()) {
      if (Objects.equal(entry.getElement(), object)) {
        return entry.getCount();
      }
    }
    return 0;
  }

  /**
   * A sensible definition of {@link #add(Object)} in terms of {@link #add(Object, int)}. If you
   * override {@link #add(Object, int)}, you may wish to override {@link #add(Object)} to forward to
   * this implementation.
   *
   * @since 7.0
   */
  protected boolean standardAdd(@ParametricNullness E element) {
    add(element, 1);
    return true;
  }

  /**
   * A sensible definition of {@link #addAll(Collection)} in terms of {@link #add(Object)} and
   * {@link #add(Object, int)}. If you override either of these methods, you may wish to override
   * {@link #addAll(Collection)} to forward to this implementation.
   *
   * @since 7.0
   */
  @Beta
  @Override
  protected boolean standardAddAll(Collection<? extends E> elementsToAdd) {
    return Multisets.addAllImpl(this, elementsToAdd);
  }

  /**
   * A sensible definition of {@link #remove(Object)} in terms of {@link #remove(Object, int)}. If
   * you override {@link #remove(Object, int)}, you may wish to override {@link #remove(Object)} to
   * forward to this implementation.
   *
   * @since 7.0
   */
  @Override
  protected boolean standardRemove(@CheckForNull Object element) {
    return remove(element, 1) > 0;
  }

  /**
   * A sensible definition of {@link #removeAll} in terms of the {@code removeAll} method of {@link
   * #elementSet}. If you override {@link #elementSet}, you may wish to override {@link #removeAll}
   * to forward to this implementation.
   *
   * @since 7.0
   */
  @Override
  protected boolean standardRemoveAll(Collection<?> elementsToRemove) {
    return Multisets.removeAllImpl(this, elementsToRemove);
  }

  /**
   * A sensible definition of {@link #retainAll} in terms of the {@code retainAll} method of {@link
   * #elementSet}. If you override {@link #elementSet}, you may wish to override {@link #retainAll}
   * to forward to this implementation.
   *
   * @since 7.0
   */
  @Override
  protected boolean standardRetainAll(Collection<?> elementsToRetain) {
    return Multisets.retainAllImpl(this, elementsToRetain);
  }

  /**
   * A sensible definition of {@link #setCount(Object, int)} in terms of {@link #count(Object)},
   * {@link #add(Object, int)}, and {@link #remove(Object, int)}. {@link #entrySet()}. If you
   * override any of these methods, you may wish to override {@link #setCount(Object, int)} to
   * forward to this implementation.
   *
   * @since 7.0
   */
  protected int standardSetCount(@ParametricNullness E element, int count) {
    return Multisets.setCountImpl(this, element, count);
  }

  /**
   * A sensible definition of {@link #setCount(Object, int, int)} in terms of {@link #count(Object)}
   * and {@link #setCount(Object, int)}. If you override either of these methods, you may wish to
   * override {@link #setCount(Object, int, int)} to forward to this implementation.
   *
   * @since 7.0
   */
  protected boolean standardSetCount(@ParametricNullness E element, int oldCount, int newCount) {
    return Multisets.setCountImpl(this, element, oldCount, newCount);
  }

  /**
   * A sensible implementation of {@link Multiset#elementSet} in terms of the following methods:
   * {@link ForwardingMultiset#clear}, {@link ForwardingMultiset#contains}, {@link
   * ForwardingMultiset#containsAll}, {@link ForwardingMultiset#count}, {@link
   * ForwardingMultiset#isEmpty}, the {@link Set#size} and {@link Set#iterator} methods of {@link
   * ForwardingMultiset#entrySet}, and {@link ForwardingMultiset#remove(Object, int)}. In many
   * situations, you may wish to override {@link ForwardingMultiset#elementSet} to forward to this
   * implementation or a subclass thereof.
   *
   * @since 10.0
   */
  @Beta
  protected class StandardElementSet extends Multisets.ElementSet<E> {
    /** Constructor for use by subclasses. */
    public StandardElementSet() {}

    @Override
    Multiset<E> multiset() {
      return ForwardingMultiset.this;
    }

    @Override
    public Iterator<E> iterator() {
      return Multisets.elementIterator(multiset().entrySet().iterator());
    }
  }

  /**
   * A sensible definition of {@link #iterator} in terms of {@link #entrySet} and {@link
   * #remove(Object)}. If you override either of these methods, you may wish to override {@link
   * #iterator} to forward to this implementation.
   *
   * @since 7.0
   */
  protected Iterator<E> standardIterator() {
    return Multisets.iteratorImpl(this);
  }

  /**
   * A sensible, albeit inefficient, definition of {@link #size} in terms of {@link #entrySet}. If
   * you override {@link #entrySet}, you may wish to override {@link #size} to forward to this
   * implementation.
   *
   * @since 7.0
   */
  protected int standardSize() {
    return Multisets.linearTimeSizeImpl(this);
  }

  /**
   * A sensible, albeit inefficient, definition of {@link #equals} in terms of {@code
   * entrySet().size()} and {@link #count}. If you override either of these methods, you may wish to
   * override {@link #equals} to forward to this implementation.
   *
   * @since 7.0
   */
  protected boolean standardEquals(@CheckForNull Object object) {
    return Multisets.equalsImpl(this, object);
  }

  /**
   * A sensible definition of {@link #hashCode} as {@code entrySet().hashCode()} . If you override
   * {@link #entrySet}, you may wish to override {@link #hashCode} to forward to this
   * implementation.
   *
   * @since 7.0
   */
  protected int standardHashCode() {
    return entrySet().hashCode();
  }

  /**
   * A sensible definition of {@link #toString} as {@code entrySet().toString()} . If you override
   * {@link #entrySet}, you may wish to override {@link #toString} to forward to this
   * implementation.
   *
   * @since 7.0
   */
  @Override
  protected String standardToString() {
    return entrySet().toString();
  }
}
