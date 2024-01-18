/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.Multiset.Entry;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.WeakOuter;
import java.io.Serializable;
import javax.annotation.CheckForNull;

/**
 * Implementation of {@link ImmutableMultiset} with zero or more elements.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true, serializable = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
@ElementTypesAreNonnullByDefault
class RegularImmutableMultiset<E> extends ImmutableMultiset<E> {
  static final RegularImmutableMultiset<Object> EMPTY =
      new RegularImmutableMultiset<>(ObjectCountHashMap.create());

  final transient ObjectCountHashMap<E> contents;
  private final transient int size;

  @LazyInit @CheckForNull private transient ImmutableSet<E> elementSet;

  RegularImmutableMultiset(ObjectCountHashMap<E> contents) {
    this.contents = contents;
    long size = 0;
    for (int i = 0; i < contents.size(); i++) {
      size += contents.getValue(i);
    }
    this.size = Ints.saturatedCast(size);
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  @Override
  public int count(@CheckForNull Object element) {
    return contents.get(element);
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public ImmutableSet<E> elementSet() {
    ImmutableSet<E> result = elementSet;
    return (result == null) ? elementSet = new ElementSet() : result;
  }

  @WeakOuter
  private final class ElementSet extends IndexedImmutableSet<E> {

    @Override
    E get(int index) {
      return contents.getKey(index);
    }

    @Override
    public boolean contains(@CheckForNull Object object) {
      return RegularImmutableMultiset.this.contains(object);
    }

    @Override
    boolean isPartialView() {
      return true;
    }

    @Override
    public int size() {
      return contents.size();
    }

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible // serialization
    @GwtIncompatible // serialization
    Object writeReplace() {
      return super.writeReplace();
    }
  }

  @Override
  Entry<E> getEntry(int index) {
    return contents.getEntry(index);
  }

  @GwtIncompatible
  private static class SerializedForm implements Serializable {
    final Object[] elements;
    final int[] counts;

    // "extends Object" works around https://github.com/typetools/checker-framework/issues/3013
    SerializedForm(Multiset<? extends Object> multiset) {
      int distinct = multiset.entrySet().size();
      elements = new Object[distinct];
      counts = new int[distinct];
      int i = 0;
      for (Entry<? extends Object> entry : multiset.entrySet()) {
        elements[i] = entry.getElement();
        counts[i] = entry.getCount();
        i++;
      }
    }

    Object readResolve() {
      ImmutableMultiset.Builder<Object> builder =
          new ImmutableMultiset.Builder<Object>(elements.length);
      for (int i = 0; i < elements.length; i++) {
        builder.addCopies(elements[i], counts[i]);
      }
      return builder.build();
    }

    private static final long serialVersionUID = 0;
  }

  @Override
  @J2ktIncompatible // serialization
  @GwtIncompatible // serialization
  Object writeReplace() {
    return new SerializedForm(this);
  }
}
