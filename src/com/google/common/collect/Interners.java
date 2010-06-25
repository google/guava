/*
 * Copyright (C) 2007 Google Inc.
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
import com.google.common.base.FinalizableReferenceQueue;
import com.google.common.base.FinalizableWeakReference;

import java.util.concurrent.ConcurrentMap;

/**
 * Contains static methods pertaining to instances of {@link Interner}.
 *
 * @author Kevin Bourrillion
 * @since 3
 */
@Beta
public final class Interners {
  private Interners() {}

  /**
   * Returns a new thread-safe interner which retains a strong reference to
   * each instance it has interned, thus preventing these instances from being
   * garbage-collected. If this retention is acceptable, this implementation may
   * perform better than {@link #newWeakInterner}.
   */
  public static <E> Interner<E> newStrongInterner() {
    final ConcurrentMap<E, E> map = new MapMaker().makeMap();
    return new Interner<E>() {
      public E intern(E sample) {
        E canonical = map.putIfAbsent(checkNotNull(sample), sample);
        return (canonical == null) ? sample : canonical;
      }
    };
  }

  /**
   * Returns a new thread-safe interner which retains a weak reference to each
   * instance it has interned, and so does not prevent these instances from
   * being garbage-collected. This most likely does not perform as well as
   * {@link #newStrongInterner}, but is the best alternative when the memory
   * usage of that implementation is unacceptable.
   */
  public static <E> Interner<E> newWeakInterner() {
    return new WeakInterner<E>();
  }

  private static class WeakInterner<E> implements Interner<E> {
    private final ConcurrentMap<InternReference, InternReference> map
        = new MapMaker().makeMap();

    public E intern(final E sample) {
      final int hashCode = sample.hashCode();

      // TODO: once MapMaker supports arbitrary Equivalence, we won't need the
      // dummy instance anymore
      Object fakeReference = new Object() {
        @Override public int hashCode() {
          return hashCode;
        }
        @Override public boolean equals(Object object) {
          if (object.hashCode() != hashCode) {
            return false;
          }
          /*
           * Implicitly an unchecked cast to WeakInterner<?>.InternReference,
           * though until OpenJDK 7, the compiler doesn't recognize this. If we
           * could explicitly cast to the wildcard type
           * WeakInterner<?>.InternReference, that would be sufficient for our
           * purposes. The compiler, however, rejects such casts (or rather, it
           * does until OpenJDK 7).
           *
           * See Sun bug 6665356.
           */
          @SuppressWarnings("unchecked")
          InternReference that = (InternReference) object;
          return sample.equals(that.get());
        }
      };

      // Fast-path; avoid creating the reference if possible
      InternReference existingRef = map.get(fakeReference);
      if (existingRef != null) {
        E canonical = existingRef.get();
        if (canonical != null) {
          return canonical;
        }
      }

      InternReference newRef = new InternReference(sample, hashCode);
      while (true) {
        InternReference sneakyRef = map.putIfAbsent(newRef, newRef);
        if (sneakyRef == null) {
          return sample;
        } else {
          E canonical = sneakyRef.get();
          if (canonical != null) {
            return canonical;
          }
        }
      }
    }

    private static final FinalizableReferenceQueue frq
        = new FinalizableReferenceQueue();

    class InternReference extends FinalizableWeakReference<E> {
      final int hashCode;

      InternReference(E key, int hash) {
        super(key, frq);
        hashCode = hash;
      }
      public void finalizeReferent() {
        map.remove(this);
      }
      @Override public E get() {
        E referent = super.get();
        if (referent == null) {
          finalizeReferent();
        }
        return referent;
      }
      @Override public int hashCode() {
        return hashCode;
      }
      @Override public boolean equals(Object object) {
        // TODO: should we try to do something to make equals() somewhat more
        // normal?
        if (object == this) {
          return true;
        }
        if (object instanceof WeakInterner.InternReference) {
          /*
           * On the following line, Eclipse wants a type parameter, producing
           * WeakInterner<?>.InternReference. The problem is that javac rejects
           * that form. Omitting WeakInterner satisfies both, though this seems
           * odd, since we are inside a WeakInterner<E> and thus the
           * WeakInterner<E> is implied, yet there is no reason to believe that
           * the other object's WeakInterner has type E. That's right -- we've
           * found a way to perform an unchecked cast without receiving a
           * warning from either Eclipse or javac. Taking advantage of that
           * seems questionable, even though we don't depend upon the type of
           * that.get(), so we'll just suppress the warning.
           */
          @SuppressWarnings("unchecked")
          WeakInterner.InternReference that =
              (WeakInterner.InternReference) object;
          if (that.hashCode != hashCode) {
            return false;
          }
          E referent = super.get();
          return referent != null && referent.equals(that.get());
        }
        return object.equals(this);
      }
    }
  }
}
