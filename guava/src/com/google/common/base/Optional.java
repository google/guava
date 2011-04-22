/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.base;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * An immutable object that may or may not contain a non-null reference to another object. Note that
 * an instance of this type never "contains {@code null}"; it either contains a non-null reference
 * or it contains nothing (the reference is "absent"). The method {@link #isPresent} distinguishes
 * between these cases.
 *
 * <p>Java's type system does not account for nullability; that is, an intentionally-nullable
 * reference has the same type as one that is expected never to be null. One reason to use a
 * non-null {@code Optional<Foo>} reference in place of a nullable {@code Foo} reference is to
 * surface this distinction to the type system; the two types are no longer interchangeable. Also,
 * the need to invoke {@link #get} serves as a reminder to either check {@link #isPresent} or
 * provide a default value, while with a nullable reference, it's easy to accidentally dereference
 * it without checking.
 *
 * <p>(Note that if you can find or create a suitable
 * <a href="http://en.wikipedia.org/wiki/Null_Object_pattern">null object</a>, you may not need
 * <i>either</i> strategy for coping with nullability.)
 *
 * <p>Other uses of this class include
 *
 * <ul>
 * <li>To distinguish between "unknown" (for example, not present in a map) and "known to have no
 *     value" (present in the map, with value {@code Optional.absent()})
 * <li>To wrap nullable references for storage in a collection that does not support {@code null}
 *     (though there are
 *     <a href="http://code.google.com/p/guava-libraries/wiki/LivingWithNullHostileCollections">
 *     several other approaches to this</a> that should be considered first)
 * </ul>
 *
 * <p>This class is not intended as a direct analogue of any existing "option" or "maybe" construct
 * from other programming environments, though it may bear some similarities.
 *
 * <p>If you are looking for a <i>mutable</i> holder class, see {@link Holder}.
 *
 * @param <T> the type of instance that can be contained. {@code Optional} is naturally covariant on
 *     this type, so it is safe to cast an {@code Optional<T>} to {@code Optional<S>} for any
 *     supertype {@code S} of {@code T}.  
 * @author Kurt Alfred Kluever
 * @author Kevin Bourrillion
 * @since Guava release 10
 */
@Beta
@GwtCompatible
public final class Optional<T> {

  // Static factories

  /**
   * Returns an {@code Optional} instance with no contained reference.
   */
  @SuppressWarnings("unchecked")
  public static <T> Optional<T> absent() {
    return (Optional<T>) ABSENT;
  }

  private static final Optional<Object> ABSENT = new Optional<Object>(null);

  /**
   * Returns an {@code Optional} instance containing the given non-null reference.
   */
  public static <T> Optional<T> of(T reference) {
    return new Optional<T>(checkNotNull(reference));
  }

  /**
   * If {@code nullableReference} is non-null, returns an {@code Optional} instance containing that
   * reference; otherwise returns {@link Optional#absent}.
   */
  public static <T> Optional<T> fromNullable(@Nullable T nullableReference) {
    return (nullableReference == null)
        ? Optional.<T>absent()
        : new Optional<T>(nullableReference);
  }

  // Constructors

  @Nullable private final T reference;

  private Optional(@Nullable T reference) {
    this.reference = reference;
  }

  // Accessors

  /**
   * Returns {@code true} if this instance contains a reference.
   */
  public boolean isPresent() {
    return reference != null;
  }

  // TODO(kevinb): isAbsent too?

  /**
   * Returns the contained non-null reference, which must be present.
   *
   * @throws IllegalStateException if the reference is absent ({@link #isPresent} returns {@code
   *     false})
   */
  public T get() {
    checkState(isPresent());
    return reference;
  }

  /**
   * Returns the contained non-null reference if it is present; {@code defaultValue} otherwise.
   */
  // TODO(kevinb): consider renaming this method
  @Nullable public T get(@Nullable T defaultValue) {
    return isPresent() ? reference : defaultValue;
  }

  // Object overrides

  /**
   * Returns {@code true} if {@code object} is an {@code Optional} instance, and either the
   * contained references are {@linkplain Object#equals equal} to each other or both are absent.
   * Note that {@code Optional} instances of differing parameterized types can be equal.
   */
  @Override public boolean equals(@Nullable Object object) {
    if (object instanceof Optional) {
      Optional<?> other = (Optional<?>) object;
      return Objects.equal(reference, other.reference);
    }
    return false;
  }

  /**
   * Returns a hash code for this instance.
   */
  @Override public int hashCode() {
    return 0x598df91c + (isPresent() ? reference.hashCode() : 0);
  }

  /**
   * Returns a string representation for this instance. The form of this string representation is
   * unspecified.
   */
  @Override public String toString() {
    return isPresent() ? "Optional.of(" + reference + ")" : "Optional.absent()";
  }
}
