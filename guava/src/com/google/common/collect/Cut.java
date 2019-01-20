/*
 * Copyright (C) 2009 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.primitives.Booleans;
import java.io.Serializable;
import java.util.NoSuchElementException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation detail for the internal structure of {@link Range} instances. Represents a unique
 * way of "cutting" a "number line" (actually of instances of type {@code C}, not necessarily
 * "numbers") into two sections; this can be done below a certain value, above a certain value,
 * below all values or above all values. With this object defined in this way, an interval can
 * always be represented by a pair of {@code Cut} instances.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible
abstract class Cut<C extends Comparable> implements Comparable<Cut<C>>, Serializable {
  final @Nullable C endpoint;

  Cut(@Nullable C endpoint) {
    this.endpoint = endpoint;
  }

  abstract boolean isLessThan(C value);

  abstract BoundType typeAsLowerBound();

  abstract BoundType typeAsUpperBound();

  abstract Cut<C> withLowerBoundType(BoundType boundType, DiscreteDomain<C> domain);

  abstract Cut<C> withUpperBoundType(BoundType boundType, DiscreteDomain<C> domain);

  abstract void describeAsLowerBound(StringBuilder sb);

  abstract void describeAsUpperBound(StringBuilder sb);

  abstract C leastValueAbove(DiscreteDomain<C> domain);

  abstract C greatestValueBelow(DiscreteDomain<C> domain);

  /*
   * The canonical form is a BelowValue cut whenever possible, otherwise ABOVE_ALL, or
   * (only in the case of types that are unbounded below) BELOW_ALL.
   */
  Cut<C> canonical(DiscreteDomain<C> domain) {
    return this;
  }

  // note: overridden by {BELOW,ABOVE}_ALL
  @Override
  public int compareTo(Cut<C> that) {
    if (that == belowAll()) {
      return 1;
    }
    if (that == aboveAll()) {
      return -1;
    }
    int result = Range.compareOrThrow(endpoint, that.endpoint);
    if (result != 0) {
      return result;
    }
    // same value. below comes before above
    return Booleans.compare(this instanceof AboveValue, that instanceof AboveValue);
  }

  C endpoint() {
    return endpoint;
  }

  @SuppressWarnings("unchecked") // catching CCE
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Cut) {
      // It might not really be a Cut<C>, but we'll catch a CCE if it's not
      Cut<C> that = (Cut<C>) obj;
      try {
        int compareResult = compareTo(that);
        return compareResult == 0;
      } catch (ClassCastException ignored) {
      }
    }
    return false;
  }

  // Prevent "missing hashCode" warning by explicitly forcing subclasses implement it
  @Override
  public abstract int hashCode();

  /*
   * The implementation neither produces nor consumes any non-null instance of type C, so
   * casting the type parameter is safe.
   */
  @SuppressWarnings("unchecked")
  static <C extends Comparable> Cut<C> belowAll() {
    return (Cut<C>) BelowAll.INSTANCE;
  }

  private static final long serialVersionUID = 0;

  private static final class BelowAll extends Cut<Comparable<?>> {
    private static final BelowAll INSTANCE = new BelowAll();

    private BelowAll() {
      super(null);
    }

    @Override
    Comparable<?> endpoint() {
      throw new IllegalStateException("range unbounded on this side");
    }

    @Override
    boolean isLessThan(Comparable<?> value) {
      return true;
    }

    @Override
    BoundType typeAsLowerBound() {
      throw new IllegalStateException();
    }

    @Override
    BoundType typeAsUpperBound() {
      throw new AssertionError("this statement should be unreachable");
    }

    @Override
    Cut<Comparable<?>> withLowerBoundType(
        BoundType boundType, DiscreteDomain<Comparable<?>> domain) {
      throw new IllegalStateException();
    }

    @Override
    Cut<Comparable<?>> withUpperBoundType(
        BoundType boundType, DiscreteDomain<Comparable<?>> domain) {
      throw new AssertionError("this statement should be unreachable");
    }

    @Override
    void describeAsLowerBound(StringBuilder sb) {
      sb.append("(-\u221e");
    }

    @Override
    void describeAsUpperBound(StringBuilder sb) {
      throw new AssertionError();
    }

    @Override
    Comparable<?> leastValueAbove(DiscreteDomain<Comparable<?>> domain) {
      return domain.minValue();
    }

    @Override
    Comparable<?> greatestValueBelow(DiscreteDomain<Comparable<?>> domain) {
      throw new AssertionError();
    }

    @Override
    Cut<Comparable<?>> canonical(DiscreteDomain<Comparable<?>> domain) {
      try {
        return Cut.<Comparable<?>>belowValue(domain.minValue());
      } catch (NoSuchElementException e) {
        return this;
      }
    }

    @Override
    public int compareTo(Cut<Comparable<?>> o) {
      return (o == this) ? 0 : -1;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }

    @Override
    public String toString() {
      return "-\u221e";
    }

    private Object readResolve() {
      return INSTANCE;
    }

    private static final long serialVersionUID = 0;
  }

  /*
   * The implementation neither produces nor consumes any non-null instance of
   * type C, so casting the type parameter is safe.
   */
  @SuppressWarnings("unchecked")
  static <C extends Comparable> Cut<C> aboveAll() {
    return (Cut<C>) AboveAll.INSTANCE;
  }

  private static final class AboveAll extends Cut<Comparable<?>> {
    private static final AboveAll INSTANCE = new AboveAll();

    private AboveAll() {
      super(null);
    }

    @Override
    Comparable<?> endpoint() {
      throw new IllegalStateException("range unbounded on this side");
    }

    @Override
    boolean isLessThan(Comparable<?> value) {
      return false;
    }

    @Override
    BoundType typeAsLowerBound() {
      throw new AssertionError("this statement should be unreachable");
    }

    @Override
    BoundType typeAsUpperBound() {
      throw new IllegalStateException();
    }

    @Override
    Cut<Comparable<?>> withLowerBoundType(
        BoundType boundType, DiscreteDomain<Comparable<?>> domain) {
      throw new AssertionError("this statement should be unreachable");
    }

    @Override
    Cut<Comparable<?>> withUpperBoundType(
        BoundType boundType, DiscreteDomain<Comparable<?>> domain) {
      throw new IllegalStateException();
    }

    @Override
    void describeAsLowerBound(StringBuilder sb) {
      throw new AssertionError();
    }

    @Override
    void describeAsUpperBound(StringBuilder sb) {
      sb.append("+\u221e)");
    }

    @Override
    Comparable<?> leastValueAbove(DiscreteDomain<Comparable<?>> domain) {
      throw new AssertionError();
    }

    @Override
    Comparable<?> greatestValueBelow(DiscreteDomain<Comparable<?>> domain) {
      return domain.maxValue();
    }

    @Override
    public int compareTo(Cut<Comparable<?>> o) {
      return (o == this) ? 0 : 1;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }

    @Override
    public String toString() {
      return "+\u221e";
    }

    private Object readResolve() {
      return INSTANCE;
    }

    private static final long serialVersionUID = 0;
  }

  static <C extends Comparable> Cut<C> belowValue(C endpoint) {
    return new BelowValue<C>(endpoint);
  }

  private static final class BelowValue<C extends Comparable> extends Cut<C> {
    BelowValue(C endpoint) {
      super(checkNotNull(endpoint));
    }

    @Override
    boolean isLessThan(C value) {
      return Range.compareOrThrow(endpoint, value) <= 0;
    }

    @Override
    BoundType typeAsLowerBound() {
      return BoundType.CLOSED;
    }

    @Override
    BoundType typeAsUpperBound() {
      return BoundType.OPEN;
    }

    @Override
    Cut<C> withLowerBoundType(BoundType boundType, DiscreteDomain<C> domain) {
      switch (boundType) {
        case CLOSED:
          return this;
        case OPEN:
          @Nullable C previous = domain.previous(endpoint);
          return (previous == null) ? Cut.<C>belowAll() : new AboveValue<C>(previous);
        default:
          throw new AssertionError();
      }
    }

    @Override
    Cut<C> withUpperBoundType(BoundType boundType, DiscreteDomain<C> domain) {
      switch (boundType) {
        case CLOSED:
          @Nullable C previous = domain.previous(endpoint);
          return (previous == null) ? Cut.<C>aboveAll() : new AboveValue<C>(previous);
        case OPEN:
          return this;
        default:
          throw new AssertionError();
      }
    }

    @Override
    void describeAsLowerBound(StringBuilder sb) {
      sb.append('[').append(endpoint);
    }

    @Override
    void describeAsUpperBound(StringBuilder sb) {
      sb.append(endpoint).append(')');
    }

    @Override
    C leastValueAbove(DiscreteDomain<C> domain) {
      return endpoint;
    }

    @Override
    C greatestValueBelow(DiscreteDomain<C> domain) {
      return domain.previous(endpoint);
    }

    @Override
    public int hashCode() {
      return endpoint.hashCode();
    }

    @Override
    public String toString() {
      return "\\" + endpoint + "/";
    }

    private static final long serialVersionUID = 0;
  }

  static <C extends Comparable> Cut<C> aboveValue(C endpoint) {
    return new AboveValue<C>(endpoint);
  }

  private static final class AboveValue<C extends Comparable> extends Cut<C> {
    AboveValue(C endpoint) {
      super(checkNotNull(endpoint));
    }

    @Override
    boolean isLessThan(C value) {
      return Range.compareOrThrow(endpoint, value) < 0;
    }

    @Override
    BoundType typeAsLowerBound() {
      return BoundType.OPEN;
    }

    @Override
    BoundType typeAsUpperBound() {
      return BoundType.CLOSED;
    }

    @Override
    Cut<C> withLowerBoundType(BoundType boundType, DiscreteDomain<C> domain) {
      switch (boundType) {
        case OPEN:
          return this;
        case CLOSED:
          @Nullable C next = domain.next(endpoint);
          return (next == null) ? Cut.<C>belowAll() : belowValue(next);
        default:
          throw new AssertionError();
      }
    }

    @Override
    Cut<C> withUpperBoundType(BoundType boundType, DiscreteDomain<C> domain) {
      switch (boundType) {
        case OPEN:
          @Nullable C next = domain.next(endpoint);
          return (next == null) ? Cut.<C>aboveAll() : belowValue(next);
        case CLOSED:
          return this;
        default:
          throw new AssertionError();
      }
    }

    @Override
    void describeAsLowerBound(StringBuilder sb) {
      sb.append('(').append(endpoint);
    }

    @Override
    void describeAsUpperBound(StringBuilder sb) {
      sb.append(endpoint).append(']');
    }

    @Override
    C leastValueAbove(DiscreteDomain<C> domain) {
      return domain.next(endpoint);
    }

    @Override
    C greatestValueBelow(DiscreteDomain<C> domain) {
      return endpoint;
    }

    @Override
    Cut<C> canonical(DiscreteDomain<C> domain) {
      C next = leastValueAbove(domain);
      return (next != null) ? belowValue(next) : Cut.<C>aboveAll();
    }

    @Override
    public int hashCode() {
      return ~endpoint.hashCode();
    }

    @Override
    public String toString() {
      return "/" + endpoint + "\\";
    }

    private static final long serialVersionUID = 0;
  }
}
