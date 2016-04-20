/*
 * Copyright (C) 2007 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.collect.MapMakerInternalMap.ReferenceEntry;

import java.util.concurrent.ConcurrentMap;

/**
 * Contains static methods pertaining to instances of {@link Interner}.
 *
 * @author Kevin Bourrillion
 * @since 3.0
 */
@Beta
@GwtIncompatible
public final class Interners {
  private Interners() {}

  /**
   * Returns a new thread-safe interner which retains a strong reference to each instance it has
   * interned, thus preventing these instances from being garbage-collected. If this retention is
   * acceptable, this implementation may perform better than {@link #newWeakInterner}.
   */
  public static <E> Interner<E> newStrongInterner() {
    final ConcurrentMap<E, E> map = new MapMaker().makeMap();
    return new Interner<E>() {
      @Override
      public E intern(E sample) {
        E canonical = map.putIfAbsent(checkNotNull(sample), sample);
        return (canonical == null) ? sample : canonical;
      }
    };
  }

  /**
   * Returns a new thread-safe interner which retains a weak reference to each instance it has
   * interned, and so does not prevent these instances from being garbage-collected. This most
   * likely does not perform as well as {@link #newStrongInterner}, but is the best alternative
   * when the memory usage of that implementation is unacceptable.
   */
  @GwtIncompatible("java.lang.ref.WeakReference")
  public static <E> Interner<E> newWeakInterner() {
    return new WeakInterner<E>();
  }

  private static class WeakInterner<E> implements Interner<E> {
    // MapMaker is our friend, we know about this type
    private final MapMakerInternalMap<E, Dummy> map =
        new MapMaker().weakKeys().keyEquivalence(Equivalence.equals()).makeCustomMap();

    @Override
    public E intern(E sample) {
      while (true) {
        // trying to read the canonical...
        ReferenceEntry<E, Dummy> entry = map.getEntry(sample);
        if (entry != null) {
          E canonical = entry.getKey();
          if (canonical != null) { // only matters if weak/soft keys are used
            return canonical;
          }
        }

        // didn't see it, trying to put it instead...
        Dummy sneaky = map.putIfAbsent(sample, Dummy.VALUE);
        if (sneaky == null) {
          return sample;
        } else {
          /* Someone beat us to it! Trying again...
           *
           * Technically this loop not guaranteed to terminate, so theoretically (extremely
           * unlikely) this thread might starve, but even then, there is always going to be another
           * thread doing progress here.
           */
        }
      }
    }

    private enum Dummy {
      VALUE
    }
  }

  /**
   * Returns a function that delegates to the {@link Interner#intern} method of the given interner.
   *
   * @since 8.0
   */
  public static <E> Function<E, E> asFunction(Interner<E> interner) {
    return new InternerFunction<E>(checkNotNull(interner));
  }

  private static class InternerFunction<E> implements Function<E, E> {

    private final Interner<E> interner;

    public InternerFunction(Interner<E> interner) {
      this.interner = interner;
    }

    @Override
    public E apply(E input) {
      return interner.intern(input);
    }

    @Override
    public int hashCode() {
      return interner.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof InternerFunction) {
        InternerFunction<?> that = (InternerFunction<?>) other;
        return interner.equals(that.interner);
      }

      return false;
    }
  }
}
