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

import com.google.common.annotations.GwtCompatible;
import com.google.common.primitives.Booleans;

import java.util.NoSuchElementException;

import javax.annotation.Nullable;

/**
 * Implementation detail for the internal structure of {@link Range} instances.
 * Represents a unique way of "cutting" a "number line" (actually of instances
 * of type {@code C}, not necessarily "numbers") into two sections; this can be
 * done below a certain value, above a certain value, below all values or above
 * all values. With this object defined in this way, an interval can always be
 * represented by a pair of {@code Cut} instances.
 *
 * @author Kevin Bourrillion
 */
@SuppressWarnings("unchecked") // allow ungenerified Comparable types
@GwtCompatible
abstract class Cut<C extends Comparable> implements Comparable<Cut<C>> {
  final C endpoint;

  Cut(C endpoint) {
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

  // the canonical form is a BelowValue cut whenever possible, otherwise
  // ABOVE_ALL, and (only in the case of types that are unbounded below)
  // BELOW_ALL.
  Cut<C> canonical(DiscreteDomain<C> domain) {
    return this;
  }

  // note: overriden by {BELOW,ABOVE}_ALL
  @Override
  public int compareTo(Cut<C> that) {
    if (that == BELOW_ALL) {
      return 1;
    }
    if (that == ABOVE_ALL) {
      return -1;
    }
    int result = compareOrThrow(endpoint, that.endpoint);
    if (result != 0) {
      return result;
    }
    // same value. below comes before above
    return Booleans.compare(
        this instanceof AboveValue, that instanceof AboveValue);
  }

  C endpoint() {
    return endpoint;
  }

  @SuppressWarnings("unchecked") // catching CCE
  @Override public boolean equals(Object obj) {
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

  static final Cut<Comparable<?>> BELOW_ALL =
      new Cut<Comparable<?>>(null) {
        @Override Comparable<?> endpoint() {
          throw new IllegalStateException("range unbounded on this side");
        }
        @Override boolean isLessThan(Comparable<?> value) {
          return true;
        }
        @Override BoundType typeAsLowerBound() {
          throw new IllegalStateException();
        }
        @Override BoundType typeAsUpperBound() {
          throw new AssertionError("this statement should be unreachable");
        }
        @Override Cut<Comparable<?>> withLowerBoundType(BoundType boundType,
            DiscreteDomain<Comparable<?>> domain) {
          throw new IllegalStateException();
        }
        @Override Cut<Comparable<?>> withUpperBoundType(BoundType boundType,
            DiscreteDomain<Comparable<?>> domain) {
          throw new AssertionError("this statement should be unreachable");
        }
        @Override void describeAsLowerBound(StringBuilder sb) {
          sb.append("(-\u221e");
        }
        @Override void describeAsUpperBound(StringBuilder sb) {
          throw new AssertionError();
        }
        @Override Comparable<?> leastValueAbove(
            DiscreteDomain<Comparable<?>> domain) {
          return domain.minValue();
        }
        @Override Comparable<?> greatestValueBelow(
            DiscreteDomain<Comparable<?>> domain) {
          throw new AssertionError();
        }
        @Override Cut<Comparable<?>> canonical(
            DiscreteDomain<Comparable<?>> domain) {
          try {
            return new BelowValue<Comparable<?>>(domain.minValue());
          } catch (NoSuchElementException e) {
            return this;
          }
        }
        @Override public int compareTo(Cut<Comparable<?>> o) {
          return (o == this) ? 0 : -1;
        }
      };

  static final Cut<Comparable<?>> ABOVE_ALL =
      new Cut<Comparable<?>>(null) {
        @Override Comparable<?> endpoint() {
          throw new IllegalStateException("range unbounded on this side");
        }
        @Override boolean isLessThan(Comparable<?> value) {
          return false;
        }
        @Override BoundType typeAsLowerBound() {
          throw new AssertionError("this statement should be unreachable");
        }
        @Override BoundType typeAsUpperBound() {
          throw new IllegalStateException();
        }
        @Override Cut<Comparable<?>> withLowerBoundType(BoundType boundType,
            DiscreteDomain<Comparable<?>> domain) {
          throw new AssertionError("this statement should be unreachable");
        }
        @Override Cut<Comparable<?>> withUpperBoundType(BoundType boundType,
            DiscreteDomain<Comparable<?>> domain) {
          throw new IllegalStateException();
        }
        @Override void describeAsLowerBound(StringBuilder sb) {
          throw new AssertionError();
        }
        @Override void describeAsUpperBound(StringBuilder sb) {
          sb.append("+\u221e)");
        }
        @Override Comparable<?> leastValueAbove(
            DiscreteDomain<Comparable<?>> domain) {
          throw new AssertionError();
        }
        @Override Comparable<?> greatestValueBelow(
            DiscreteDomain<Comparable<?>> domain) {
          return domain.maxValue();
        }
        @Override public int compareTo(Cut<Comparable<?>> o) {
          return (o == this) ? 0 : 1;
        }
      };

  static final class BelowValue<C extends Comparable> extends Cut<C> {
    BelowValue(C endpoint) {
      super(checkNotNull(endpoint));
    }

    @Override boolean isLessThan(C value) {
      return compareOrThrow(endpoint, value) <= 0;
    }
    @Override BoundType typeAsLowerBound() {
      return BoundType.CLOSED;
    }
    @Override BoundType typeAsUpperBound() {
      return BoundType.OPEN;
    }
    @Override Cut<C> withLowerBoundType(BoundType boundType, DiscreteDomain<C> domain) {
      switch (boundType) {
        case CLOSED:
          return this;
        case OPEN:
          @Nullable C previous = domain.previous(endpoint);
          return (Cut<C>) ((previous == null) ? BELOW_ALL : new AboveValue<C>(previous));
        default:
          throw new AssertionError();
      }
    }
    @Override Cut<C> withUpperBoundType(BoundType boundType, DiscreteDomain<C> domain) {
      switch (boundType) {
        case CLOSED:
          @Nullable C previous = domain.previous(endpoint);
          return (Cut<C>) ((previous == null) ? ABOVE_ALL : new AboveValue<C>(previous));
        case OPEN:
          return this;
        default:
          throw new AssertionError();
      }
    }
    @Override void describeAsLowerBound(StringBuilder sb) {
      sb.append('[').append(endpoint);
    }
    @Override void describeAsUpperBound(StringBuilder sb) {
      sb.append(endpoint).append(')');
    }
    @Override C leastValueAbove(DiscreteDomain<C> domain) {
      return endpoint;
    }
    @Override C greatestValueBelow(DiscreteDomain<C> domain) {
      return domain.previous(endpoint);
    }
    @Override public int hashCode() {
      return endpoint.hashCode();
    }
  }

  static final class AboveValue<C extends Comparable> extends Cut<C> {
    AboveValue(C endpoint) {
      super(checkNotNull(endpoint));
    }

    @Override boolean isLessThan(C value) {
      return compareOrThrow(endpoint, value) < 0;
    }
    @Override BoundType typeAsLowerBound() {
      return BoundType.OPEN;
    }
    @Override BoundType typeAsUpperBound() {
      return BoundType.CLOSED;
    }
    @Override Cut<C> withLowerBoundType(BoundType boundType, DiscreteDomain<C> domain) {
      switch (boundType) {
        case OPEN:
          return this;
        case CLOSED:
          @Nullable C next = domain.next(endpoint);
          return (Cut<C>) ((next == null) ? BELOW_ALL : new BelowValue<C>(next));
        default:
          throw new AssertionError();
      }
    }
    @Override Cut<C> withUpperBoundType(BoundType boundType, DiscreteDomain<C> domain) {
      switch (boundType) {
        case OPEN:
          @Nullable C next = domain.next(endpoint);
          return (Cut<C>) ((next == null) ? ABOVE_ALL : new BelowValue<C>(next));
        case CLOSED:
          return this;
        default:
          throw new AssertionError();
      }
    }
    @Override void describeAsLowerBound(StringBuilder sb) {
      sb.append('(').append(endpoint);
    }
    @Override void describeAsUpperBound(StringBuilder sb) {
      sb.append(endpoint).append(']');
    }
    @Override C leastValueAbove(DiscreteDomain<C> domain) {
      return domain.next(endpoint);
    }
    @Override C greatestValueBelow(DiscreteDomain<C> domain) {
      return endpoint;
    }
    @Override Cut<C> canonical(DiscreteDomain<C> domain) {
      C next = leastValueAbove(domain);
      if (next != null) {
        return new BelowValue<C>(next);
      }

      Cut<C> aboveAll = (Cut<C>) ABOVE_ALL;
      return aboveAll;
    }
    @Override public int hashCode() {
      return ~endpoint.hashCode();
    }
  }

  @SuppressWarnings("unchecked") // this method may throw CCE
  private static int compareOrThrow(Comparable left, Comparable right) {
    return left.compareTo(right);
  }
}
