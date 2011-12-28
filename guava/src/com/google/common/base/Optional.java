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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * An immutable object that may contain a non-null reference to another object. Each
 * instance of this type either contains a non-null reference, or contains nothing (in
 * which case we say that the reference is "absent"); it is never said to "contain {@code
 * null}".
 *
 * <p>A non-null {@code Optional<T>} reference can be used as a replacement for a nullable
 * {@code T} reference. It allows you to represent "a {@code T} that must be present" and
 * a "a {@code T} that might be absent" as two distinct types in your program, which can
 * aid clarity.
 *
 * <p>Some uses of this class include
 *
 * <ul>
 * <li>As a method return type, as an alternative to returning {@code null} to indicate
 *     that no value was available
 * <li>To distinguish between "unknown" (for example, not present in a map) and "known to
 *     have no value" (present in the map, with value {@code Optional.absent()})
 * <li>To wrap nullable references for storage in a collection that does not support
 *     {@code null} (though there are
 *     <a href="http://code.google.com/p/guava-libraries/wiki/LivingWithNullHostileCollections">
 *     several other approaches to this</a> that should be considered first)
 * </ul>
 *
 * <p>A common alternative to using this class is to find or create a suitable
 * <a href="http://en.wikipedia.org/wiki/Null_Object_pattern">null object</a> for the
 * type in question.
 *
 * <p>This class is not intended as a direct analogue of any existing "option" or "maybe"
 * construct from other programming environments, though it may bear some similarities.
 *
 * @param <T> the type of instance that can be contained. {@code Optional} is naturally
 *     covariant on this type, so it is safe to cast an {@code Optional<T>} to {@code
 *     Optional<S>} for any supertype {@code S} of {@code T}.
 * @author Kurt Alfred Kluever
 * @author Kevin Bourrillion
 * @since 10.0
 */
@Beta
@GwtCompatible
public abstract class Optional<T> implements Serializable {
  /**
   * Returns an {@code Optional} instance with no contained reference.
   */
  @SuppressWarnings("unchecked")
  public static <T> Optional<T> absent() {
    return (Optional<T>) Absent.INSTANCE;
  }

  /**
   * Returns an {@code Optional} instance containing the given non-null reference.
   */
  public static <T> Optional<T> of(T reference) {
    return new Present<T>(checkNotNull(reference));
  }

  /**
   * If {@code nullableReference} is non-null, returns an {@code Optional} instance containing that
   * reference; otherwise returns {@link Optional#absent}.
   */
  public static <T> Optional<T> fromNullable(@Nullable T nullableReference) {
    return (nullableReference == null)
        ? Optional.<T>absent()
        : new Present<T>(nullableReference);
  }

  private Optional() {}

  /**
   * Returns {@code true} if this holder contains a (non-null) instance.
   */
  public abstract boolean isPresent();

  /**
   * Returns the contained instance, which must be present. If the instance might be
   * absent, use {@link #or(Object)} or {@link #orNull} instead.
   *
   * @throws IllegalStateException if the instance is absent ({@link #isPresent} returns
   *     {@code false})
   */
  public abstract T get();

  /**
   * Returns the contained instance if it is present; {@code defaultValue} otherwise. If
   * no default value should be required because the instance is known to be present, use
   * {@link #get()} instead. For a default value of {@code null}, use {@link #orNull}.
   */
  public abstract T or(T defaultValue);

  /**
   * Returns this {@code Optional} if it has a value present; {@code secondChoice}
   * otherwise.
   */
  public abstract Optional<T> or(Optional<? extends T> secondChoice);

  /**
   * Returns the contained instance if it is present; {@code supplier.get()} otherwise. If the
   * supplier returns {@code null}, a {@link NullPointerException} will be thrown.
   *
   * @throws NullPointerException if the supplier returns {@code null}
   */
  public abstract T or(Supplier<? extends T> supplier);

  /**
   * Returns the contained instance if it is present; {@code null} otherwise. If the
   * instance is known to be present, use {@link #get()} instead.
   */
  @Nullable public abstract T orNull();

  /**
   * Returns an immutable singleton {@link Set} whose only element is the
   * contained instance if it is present; an empty immutable {@link Set}
   * otherwise.
   *
   * @since 11.0
   */
  public abstract Set<T> asSet();

  /**
   * Returns {@code true} if {@code object} is an {@code Optional} instance, and either
   * the contained references are {@linkplain Object#equals equal} to each other or both
   * are absent. Note that {@code Optional} instances of differing parameterized types can
   * be equal.
   */
  @Override public abstract boolean equals(@Nullable Object object);

  /**
   * Returns a hash code for this instance.
   */
  @Override public abstract int hashCode();

  /**
   * Returns a string representation for this instance. The form of this string
   * representation is unspecified.
   */
  @Override public abstract String toString();

  /**
   * Returns the value of each present instance from the supplied {@code optionals}, in order,
   * skipping over occurrences of {@link Optional#absent}. Iterators are unmodifiable and are
   * evaluated lazily.
   *
   * @since 11.0
   */
  public static <T> Iterable<T> presentInstances(final Iterable<Optional<T>> optionals) {
    checkNotNull(optionals);
    return new Iterable<T>() {
      @Override public Iterator<T> iterator() {
        return new AbstractIterator<T>() {
          private final Iterator<Optional<T>> iterator = checkNotNull(optionals.iterator());

          @Override protected T computeNext() {
            while (iterator.hasNext()) {
              Optional<T> optional = iterator.next();
              if (optional.isPresent()) {
                return optional.get();
              }
            }
            return endOfData();
          }
        };
      };
    };
  }

  private static final long serialVersionUID = 0;

  private static final class Present<T> extends Optional<T> {
    private final T reference;

    Present(T reference) {
      this.reference = reference;
    }

    @Override public boolean isPresent() {
      return true;
    }

    @Override public T get() {
      return reference;
    }

    @Override public T or(T defaultValue) {
      checkNotNull(defaultValue, "use orNull() instead of or(null)");
      return reference;
    }

    @Override public Optional<T> or(Optional<? extends T> secondChoice) {
      checkNotNull(secondChoice);
      return this;
    }

    @Override public T or(Supplier<? extends T> supplier) {
      checkNotNull(supplier);
      return reference;
    }

    @Override public T orNull() {
      return reference;
    }

    @Override public Set<T> asSet() {
      return Collections.singleton(reference);
    }

    @Override public boolean equals(@Nullable Object object) {
      if (object instanceof Present) {
        Present<?> other = (Present<?>) object;
        return reference.equals(other.reference);
      }
      return false;
    }

    @Override public int hashCode() {
      return 0x598df91c + reference.hashCode();
    }

    @Override public String toString() {
      return "Optional.of(" + reference + ")";
    }

    private static final long serialVersionUID = 0;
  }

  private static final class Absent extends Optional<Object> {
    private static final Absent INSTANCE = new Absent();

    @Override public boolean isPresent() {
      return false;
    }

    @Override public Object get() {
      throw new IllegalStateException("value is absent");
    }

    @Override public Object or(Object defaultValue) {
      return checkNotNull(defaultValue, "use orNull() instead of or(null)");
    }

    @SuppressWarnings("unchecked") // safe covariant cast
    @Override public Optional<Object> or(Optional<?> secondChoice) {
      return (Optional) checkNotNull(secondChoice);
    }

    @Override public Object or(Supplier<?> supplier) {
      return checkNotNull(supplier.get(),
          "use orNull() instead of a Supplier that returns null");
    }

    @Override @Nullable public Object orNull() {
      return null;
    }

    @Override public Set<Object> asSet() {
      return Collections.emptySet();
    }

    @Override public boolean equals(@Nullable Object object) {
      return object == this;
    }

    @Override public int hashCode() {
      return 0x598df91c;
    }

    @Override public String toString() {
      return "Optional.absent()";
    }

    private Object readResolve() {
      return INSTANCE;
    }

    private static final long serialVersionUID = 0;
  }
}
