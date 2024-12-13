/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.base;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An object which joins pieces of text (specified as an array, {@link Iterable}, varargs or even a
 * {@link Map}) with a separator. It either appends the results to an {@link Appendable} or returns
 * them as a {@link String}. Example:
 *
 * <pre>{@code
 * Joiner joiner = Joiner.on("; ").skipNulls();
 *  . . .
 * return joiner.join("Harry", null, "Ron", "Hermione");
 * }</pre>
 *
 * <p>This returns the string {@code "Harry; Ron; Hermione"}. Note that all input elements are
 * converted to strings using {@link Object#toString()} before being appended.
 *
 * <p>If neither {@link #skipNulls()} nor {@link #useForNull(String)} is specified, the joining
 * methods will throw {@link NullPointerException} if any given element is null.
 *
 * <p><b>Warning: joiner instances are always immutable</b>; a configuration method such as {@code
 * useForNull} has no effect on the instance it is invoked on! You must store and use the new joiner
 * instance returned by the method. This makes joiners thread-safe, and safe to store as {@code
 * static final} constants.
 *
 * <pre>{@code
 * // Bad! Do not do this!
 * Joiner joiner = Joiner.on(',');
 * joiner.skipNulls(); // does nothing!
 * return joiner.join("wrong", null, "wrong");
 * }</pre>
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/StringsExplained#joiner">{@code Joiner}</a>.
 *
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public class Joiner {
  /** Returns a joiner which automatically places {@code separator} between consecutive elements. */
  public static Joiner on(String separator) {
    return new Joiner(separator);
  }

  /** Returns a joiner which automatically places {@code separator} between consecutive elements. */
  public static Joiner on(char separator) {
    return new Joiner(String.valueOf(separator));
  }

  private final String separator;

  private Joiner(String separator) {
    this.separator = checkNotNull(separator);
  }

  private Joiner(Joiner prototype) {
    this.separator = prototype.separator;
  }

  /*
   * In this file, we use <? extends @Nullable Object> instead of <?> to work around a Kotlin bug
   * (see b/189937072 until we file a bug against Kotlin itself). (The two should be equivalent, so
   * we normally prefer the shorter one.)
   */

  /**
   * Appends the string representation of each of {@code parts}, using the previously configured
   * separator between each, to {@code appendable}.
   */
  @CanIgnoreReturnValue
  public <A extends Appendable> A appendTo(A appendable, Iterable<? extends @Nullable Object> parts)
      throws IOException {
    return appendTo(appendable, parts.iterator());
  }

  /**
   * Appends the string representation of each of {@code parts}, using the previously configured
   * separator between each, to {@code appendable}.
   *
   * @since 11.0
   */
  @CanIgnoreReturnValue
  public <A extends Appendable> A appendTo(A appendable, Iterator<? extends @Nullable Object> parts)
      throws IOException {
    checkNotNull(appendable);
    if (parts.hasNext()) {
      appendable.append(toString(parts.next()));
      while (parts.hasNext()) {
        appendable.append(separator);
        appendable.append(toString(parts.next()));
      }
    }
    return appendable;
  }

  /**
   * Appends the string representation of each of {@code parts}, using the previously configured
   * separator between each, to {@code appendable}.
   */
  @CanIgnoreReturnValue
  public final <A extends Appendable> A appendTo(A appendable, @Nullable Object[] parts)
      throws IOException {
    @SuppressWarnings("nullness") // TODO: b/316358623 - Remove suppression after fixing checker
    List<?> partsList = Arrays.<@Nullable Object>asList(parts);
    return appendTo(appendable, partsList);
  }

  /** Appends to {@code appendable} the string representation of each of the remaining arguments. */
  @CanIgnoreReturnValue
  public final <A extends Appendable> A appendTo(
      A appendable,
      @CheckForNull Object first,
      @CheckForNull Object second,
      @Nullable Object... rest)
      throws IOException {
    return appendTo(appendable, iterable(first, second, rest));
  }

  /**
   * Appends the string representation of each of {@code parts}, using the previously configured
   * separator between each, to {@code builder}. Identical to {@link #appendTo(Appendable,
   * Iterable)}, except that it does not throw {@link IOException}.
   */
  @CanIgnoreReturnValue
  public final StringBuilder appendTo(
      StringBuilder builder, Iterable<? extends @Nullable Object> parts) {
    return appendTo(builder, parts.iterator());
  }

  /**
   * Appends the string representation of each of {@code parts}, using the previously configured
   * separator between each, to {@code builder}. Identical to {@link #appendTo(Appendable,
   * Iterable)}, except that it does not throw {@link IOException}.
   *
   * @since 11.0
   */
  @CanIgnoreReturnValue
  public final StringBuilder appendTo(
      StringBuilder builder, Iterator<? extends @Nullable Object> parts) {
    try {
      appendTo((Appendable) builder, parts);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
    return builder;
  }

  /**
   * Appends the string representation of each of {@code parts}, using the previously configured
   * separator between each, to {@code builder}. Identical to {@link #appendTo(Appendable,
   * Iterable)}, except that it does not throw {@link IOException}.
   */
  @CanIgnoreReturnValue
  public final StringBuilder appendTo(StringBuilder builder, @Nullable Object[] parts) {
    @SuppressWarnings("nullness") // TODO: b/316358623 - Remove suppression after fixing checker
    List<?> partsList = Arrays.<@Nullable Object>asList(parts);
    return appendTo(builder, partsList);
  }

  /**
   * Appends to {@code builder} the string representation of each of the remaining arguments.
   * Identical to {@link #appendTo(Appendable, Object, Object, Object...)}, except that it does not
   * throw {@link IOException}.
   */
  @CanIgnoreReturnValue
  public final StringBuilder appendTo(
      StringBuilder builder,
      @CheckForNull Object first,
      @CheckForNull Object second,
      @Nullable Object... rest) {
    return appendTo(builder, iterable(first, second, rest));
  }

  /**
   * Returns a string containing the string representation of each of {@code parts}, using the
   * previously configured separator between each.
   */
  public final String join(Iterable<? extends @Nullable Object> parts) {
    return join(parts.iterator());
  }

  /**
   * Returns a string containing the string representation of each of {@code parts}, using the
   * previously configured separator between each.
   *
   * @since 11.0
   */
  public final String join(Iterator<? extends @Nullable Object> parts) {
    return appendTo(new StringBuilder(), parts).toString();
  }

  /**
   * Returns a string containing the string representation of each of {@code parts}, using the
   * previously configured separator between each.
   */
  public final String join(@Nullable Object[] parts) {
    @SuppressWarnings("nullness") // TODO: b/316358623 - Remove suppression after fixing checker
    List<?> partsList = Arrays.<@Nullable Object>asList(parts);
    return join(partsList);
  }

  /**
   * Returns a string containing the string representation of each argument, using the previously
   * configured separator between each.
   */
  public final String join(
      @CheckForNull Object first, @CheckForNull Object second, @Nullable Object... rest) {
    return join(iterable(first, second, rest));
  }

  /**
   * Returns a joiner with the same behavior as this one, except automatically substituting {@code
   * nullText} for any provided null elements.
   */
  public Joiner useForNull(String nullText) {
    checkNotNull(nullText);
    return new Joiner(this) {
      @Override
      CharSequence toString(@CheckForNull Object part) {
        return (part == null) ? nullText : Joiner.this.toString(part);
      }

      @Override
      public Joiner useForNull(String nullText) {
        throw new UnsupportedOperationException("already specified useForNull");
      }

      @Override
      public Joiner skipNulls() {
        throw new UnsupportedOperationException("already specified useForNull");
      }
    };
  }

  /**
   * Returns a joiner with the same behavior as this joiner, except automatically skipping over any
   * provided null elements.
   */
  public Joiner skipNulls() {
    return new Joiner(this) {
      @Override
      public <A extends Appendable> A appendTo(
          A appendable, Iterator<? extends @Nullable Object> parts) throws IOException {
        checkNotNull(appendable, "appendable");
        checkNotNull(parts, "parts");
        while (parts.hasNext()) {
          Object part = parts.next();
          if (part != null) {
            appendable.append(Joiner.this.toString(part));
            break;
          }
        }
        while (parts.hasNext()) {
          Object part = parts.next();
          if (part != null) {
            appendable.append(separator);
            appendable.append(Joiner.this.toString(part));
          }
        }
        return appendable;
      }

      @Override
      public Joiner useForNull(String nullText) {
        throw new UnsupportedOperationException("already specified skipNulls");
      }

      @Override
      public MapJoiner withKeyValueSeparator(String kvs) {
        throw new UnsupportedOperationException("can't use .skipNulls() with maps");
      }
    };
  }

  /**
   * Returns a {@code MapJoiner} using the given key-value separator, and the same configuration as
   * this {@code Joiner} otherwise.
   *
   * @since 20.0
   */
  public MapJoiner withKeyValueSeparator(char keyValueSeparator) {
    return withKeyValueSeparator(String.valueOf(keyValueSeparator));
  }

  /**
   * Returns a {@code MapJoiner} using the given key-value separator, and the same configuration as
   * this {@code Joiner} otherwise.
   */
  public MapJoiner withKeyValueSeparator(String keyValueSeparator) {
    return new MapJoiner(this, keyValueSeparator);
  }

  /**
   * An object that joins map entries in the same manner as {@code Joiner} joins iterables and
   * arrays. Like {@code Joiner}, it is thread-safe and immutable.
   *
   * <p>In addition to operating on {@code Map} instances, {@code MapJoiner} can operate on {@code
   * Multimap} entries in two distinct modes:
   *
   * <ul>
   *   <li>To output a separate entry for each key-value pair, pass {@code multimap.entries()} to a
   *       {@code MapJoiner} method that accepts entries as input, and receive output of the form
   *       {@code key1=A&key1=B&key2=C}.
   *   <li>To output a single entry for each key, pass {@code multimap.asMap()} to a {@code
   *       MapJoiner} method that accepts a map as input, and receive output of the form {@code
   *       key1=[A, B]&key2=C}.
   * </ul>
   *
   * @since 2.0
   */
  public static final class MapJoiner {
    private final Joiner joiner;
    private final String keyValueSeparator;

    private MapJoiner(Joiner joiner, String keyValueSeparator) {
      this.joiner = joiner; // only "this" is ever passed, so don't checkNotNull
      this.keyValueSeparator = checkNotNull(keyValueSeparator);
    }

    /**
     * Appends the string representation of each entry of {@code map}, using the previously
     * configured separator and key-value separator, to {@code appendable}.
     */
    @CanIgnoreReturnValue
    public <A extends Appendable> A appendTo(A appendable, Map<?, ?> map) throws IOException {
      return appendTo(appendable, map.entrySet());
    }

    /**
     * Appends the string representation of each entry of {@code map}, using the previously
     * configured separator and key-value separator, to {@code builder}. Identical to {@link
     * #appendTo(Appendable, Map)}, except that it does not throw {@link IOException}.
     */
    @CanIgnoreReturnValue
    public StringBuilder appendTo(StringBuilder builder, Map<?, ?> map) {
      return appendTo(builder, map.entrySet());
    }

    /**
     * Appends the string representation of each entry in {@code entries}, using the previously
     * configured separator and key-value separator, to {@code appendable}.
     *
     * @since 10.0
     */
    @CanIgnoreReturnValue
    public <A extends Appendable> A appendTo(A appendable, Iterable<? extends Entry<?, ?>> entries)
        throws IOException {
      return appendTo(appendable, entries.iterator());
    }

    /**
     * Appends the string representation of each entry in {@code entries}, using the previously
     * configured separator and key-value separator, to {@code appendable}.
     *
     * @since 11.0
     */
    @CanIgnoreReturnValue
    public <A extends Appendable> A appendTo(A appendable, Iterator<? extends Entry<?, ?>> parts)
        throws IOException {
      checkNotNull(appendable);
      if (parts.hasNext()) {
        Entry<?, ?> entry = parts.next();
        appendable.append(joiner.toString(entry.getKey()));
        appendable.append(keyValueSeparator);
        appendable.append(joiner.toString(entry.getValue()));
        while (parts.hasNext()) {
          appendable.append(joiner.separator);
          Entry<?, ?> e = parts.next();
          appendable.append(joiner.toString(e.getKey()));
          appendable.append(keyValueSeparator);
          appendable.append(joiner.toString(e.getValue()));
        }
      }
      return appendable;
    }

    /**
     * Appends the string representation of each entry in {@code entries}, using the previously
     * configured separator and key-value separator, to {@code builder}. Identical to {@link
     * #appendTo(Appendable, Iterable)}, except that it does not throw {@link IOException}.
     *
     * @since 10.0
     */
    @CanIgnoreReturnValue
    public StringBuilder appendTo(StringBuilder builder, Iterable<? extends Entry<?, ?>> entries) {
      return appendTo(builder, entries.iterator());
    }

    /**
     * Appends the string representation of each entry in {@code entries}, using the previously
     * configured separator and key-value separator, to {@code builder}. Identical to {@link
     * #appendTo(Appendable, Iterable)}, except that it does not throw {@link IOException}.
     *
     * @since 11.0
     */
    @CanIgnoreReturnValue
    public StringBuilder appendTo(StringBuilder builder, Iterator<? extends Entry<?, ?>> entries) {
      try {
        appendTo((Appendable) builder, entries);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
      return builder;
    }

    /**
     * Returns a string containing the string representation of each entry of {@code map}, using the
     * previously configured separator and key-value separator.
     */
    public String join(Map<?, ?> map) {
      return join(map.entrySet());
    }

    /**
     * Returns a string containing the string representation of each entry in {@code entries}, using
     * the previously configured separator and key-value separator.
     *
     * @since 10.0
     */
    public String join(Iterable<? extends Entry<?, ?>> entries) {
      return join(entries.iterator());
    }

    /**
     * Returns a string containing the string representation of each entry in {@code entries}, using
     * the previously configured separator and key-value separator.
     *
     * @since 11.0
     */
    public String join(Iterator<? extends Entry<?, ?>> entries) {
      return appendTo(new StringBuilder(), entries).toString();
    }

    /**
     * Returns a map joiner with the same behavior as this one, except automatically substituting
     * {@code nullText} for any provided null keys or values.
     */
    public MapJoiner useForNull(String nullText) {
      return new MapJoiner(joiner.useForNull(nullText), keyValueSeparator);
    }
  }

  CharSequence toString(@CheckForNull Object part) {
    /*
     * requireNonNull is not safe: Joiner.on(...).join(somethingThatContainsNull) will indeed throw.
     * However, Joiner.on(...).useForNull(...).join(somethingThatContainsNull) *is* safe -- because
     * it returns a subclass of Joiner that overrides this method to tolerate null inputs.
     *
     * Unfortunately, we don't distinguish between these two cases in our public API: Joiner.on(...)
     * and Joiner.on(...).useForNull(...) both declare the same return type: plain Joiner. To ensure
     * that users *can* pass null arguments to Joiner, we annotate it as if it always tolerates null
     * inputs, rather than as if it never tolerates them.
     *
     * We rely on checkers to implement special cases to catch dangerous calls to join(), etc. based
     * on what they know about the particular Joiner instances the calls are performed on.
     *
     * (In addition to useForNull, we also offer skipNulls. It, too, tolerates null inputs, but its
     * tolerance is implemented differently: Its implementation avoids calling this toString(Object)
     * method in the first place.)
     */
    requireNonNull(part);
    return (part instanceof CharSequence) ? (CharSequence) part : part.toString();
  }

  private static Iterable<@Nullable Object> iterable(
      @CheckForNull Object first, @CheckForNull Object second, @Nullable Object[] rest) {
    checkNotNull(rest);
    return new AbstractList<@Nullable Object>() {
      @Override
      public int size() {
        return rest.length + 2;
      }

      @Override
      @CheckForNull
      public Object get(int index) {
        switch (index) {
          case 0:
            return first;
          case 1:
            return second;
          default:
            return rest[index - 2];
        }
      }
    };
  }
}
