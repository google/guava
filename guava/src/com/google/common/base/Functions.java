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

package com.google.common.base;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Static utility methods pertaining to {@code com.google.common.base.Function} instances; see that
 * class for information about migrating to {@code java.util.function}.
 *
 * <p>All methods return serializable functions as long as they're given serializable parameters.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/FunctionalExplained">the use of {@code Function}</a>.
 *
 * @author Mike Bostock
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible
public final class Functions {
  private Functions() {}

  /**
   * A function equivalent to the method reference {@code Object::toString}, for users not yet using
   * Java 8. The function simply invokes {@code toString} on its argument and returns the result. It
   * throws a {@link NullPointerException} on null input.
   *
   * <p><b>Warning:</b> The returned function may not be <i>consistent with equals</i> (as
   * documented at {@link Function#apply}). For example, this function yields different results for
   * the two equal instances {@code ImmutableSet.of(1, 2)} and {@code ImmutableSet.of(2, 1)}.
   *
   * <p><b>Warning:</b> as with all function types in this package, avoid depending on the specific
   * {@code equals}, {@code hashCode} or {@code toString} behavior of the returned function. A
   * future migration to {@code java.util.function} will not preserve this behavior.
   *
   * <p><b>For Java 8 users:</b> use the method reference {@code Object::toString} instead. In the
   * future, when this class requires Java 8, this method will be deprecated. See {@link Function}
   * for more important information about the Java 8 transition.
   */
  public static Function<Object, String> toStringFunction() {
    return ToStringFunction.INSTANCE;
  }

  // enum singleton pattern
  private enum ToStringFunction implements Function<Object, String> {
    INSTANCE;

    @Override
    public String apply(Object o) {
      checkNotNull(o); // eager for GWT.
      return o.toString();
    }

    @Override
    public String toString() {
      return "Functions.toStringFunction()";
    }
  }

  /** Returns the identity function. */
  // implementation is "fully variant"; E has become a "pass-through" type
  @SuppressWarnings("unchecked")
  public static <E> Function<E, E> identity() {
    return (Function<E, E>) IdentityFunction.INSTANCE;
  }

  // enum singleton pattern
  private enum IdentityFunction implements Function<Object, Object> {
    INSTANCE;

    @Override
    public @Nullable Object apply(@Nullable Object o) {
      return o;
    }

    @Override
    public String toString() {
      return "Functions.identity()";
    }
  }

  /**
   * Returns a function which performs a map lookup. The returned function throws an {@link
   * IllegalArgumentException} if given a key that does not exist in the map. See also {@link
   * #forMap(Map, Object)}, which returns a default value in this case.
   *
   * <p>Note: if {@code map} is a {@link com.google.common.collect.BiMap BiMap} (or can be one), you
   * can use {@link com.google.common.collect.Maps#asConverter Maps.asConverter} instead to get a
   * function that also supports reverse conversion.
   *
   * <p><b>Java 8 users:</b> if you are okay with {@code null} being returned for an unrecognized
   * key (instead of an exception being thrown), you can use the method reference {@code map::get}
   * instead.
   */
  public static <K, V> Function<K, V> forMap(Map<K, V> map) {
    return new FunctionForMapNoDefault<>(map);
  }

  /**
   * Returns a function which performs a map lookup with a default value. The function created by
   * this method returns {@code defaultValue} for all inputs that do not belong to the map's key
   * set. See also {@link #forMap(Map)}, which throws an exception in this case.
   *
   * <p><b>Java 8 users:</b> you can just write the lambda expression {@code k ->
   * map.getOrDefault(k, defaultValue)} instead.
   *
   * @param map source map that determines the function behavior
   * @param defaultValue the value to return for inputs that aren't map keys
   * @return function that returns {@code map.get(a)} when {@code a} is a key, or {@code
   *     defaultValue} otherwise
   */
  public static <K, V> Function<K, V> forMap(Map<K, ? extends V> map, @Nullable V defaultValue) {
    return new ForMapWithDefault<>(map, defaultValue);
  }

  private static class FunctionForMapNoDefault<K, V> implements Function<K, V>, Serializable {
    final Map<K, V> map;

    FunctionForMapNoDefault(Map<K, V> map) {
      this.map = checkNotNull(map);
    }

    @Override
    public V apply(@Nullable K key) {
      V result = map.get(key);
      checkArgument(result != null || map.containsKey(key), "Key '%s' not present in map", key);
      return result;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o instanceof FunctionForMapNoDefault) {
        FunctionForMapNoDefault<?, ?> that = (FunctionForMapNoDefault<?, ?>) o;
        return map.equals(that.map);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return map.hashCode();
    }

    @Override
    public String toString() {
      return "Functions.forMap(" + map + ")";
    }

    private static final long serialVersionUID = 0;
  }

  private static class ForMapWithDefault<K, V> implements Function<K, V>, Serializable {
    final Map<K, ? extends V> map;
    final @Nullable V defaultValue;

    ForMapWithDefault(Map<K, ? extends V> map, @Nullable V defaultValue) {
      this.map = checkNotNull(map);
      this.defaultValue = defaultValue;
    }

    @Override
    public V apply(@Nullable K key) {
      V result = map.get(key);
      return (result != null || map.containsKey(key)) ? result : defaultValue;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o instanceof ForMapWithDefault) {
        ForMapWithDefault<?, ?> that = (ForMapWithDefault<?, ?>) o;
        return map.equals(that.map) && Objects.equal(defaultValue, that.defaultValue);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(map, defaultValue);
    }

    @Override
    public String toString() {
      // TODO(cpovirk): maybe remove "defaultValue=" to make this look like the method call does
      return "Functions.forMap(" + map + ", defaultValue=" + defaultValue + ")";
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns the composition of two functions. For {@code f: A->B} and {@code g: B->C}, composition
   * is defined as the function h such that {@code h(a) == g(f(a))} for each {@code a}.
   *
   * <p><b>Java 8 users:</b> use {@code g.compose(f)} or (probably clearer) {@code f.andThen(g)}
   * instead.
   *
   * @param g the second function to apply
   * @param f the first function to apply
   * @return the composition of {@code f} and {@code g}
   * @see <a href="//en.wikipedia.org/wiki/Function_composition">function composition</a>
   */
  public static <A, B, C> Function<A, C> compose(Function<B, C> g, Function<A, ? extends B> f) {
    return new FunctionComposition<>(g, f);
  }

  private static class FunctionComposition<A, B, C> implements Function<A, C>, Serializable {
    private final Function<B, C> g;
    private final Function<A, ? extends B> f;

    public FunctionComposition(Function<B, C> g, Function<A, ? extends B> f) {
      this.g = checkNotNull(g);
      this.f = checkNotNull(f);
    }

    @Override
    public C apply(@Nullable A a) {
      return g.apply(f.apply(a));
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof FunctionComposition) {
        FunctionComposition<?, ?, ?> that = (FunctionComposition<?, ?, ?>) obj;
        return f.equals(that.f) && g.equals(that.g);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return f.hashCode() ^ g.hashCode();
    }

    @Override
    public String toString() {
      // TODO(cpovirk): maybe make this look like the method call does ("Functions.compose(...)")
      return g + "(" + f + ")";
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Creates a function that returns the same boolean output as the given predicate for all inputs.
   *
   * <p>The returned function is <i>consistent with equals</i> (as documented at {@link
   * Function#apply}) if and only if {@code predicate} is itself consistent with equals.
   *
   * <p><b>Java 8 users:</b> use the method reference {@code predicate::test} instead.
   */
  public static <T> Function<T, Boolean> forPredicate(Predicate<T> predicate) {
    return new PredicateFunction<T>(predicate);
  }

  /** @see Functions#forPredicate */
  private static class PredicateFunction<T> implements Function<T, Boolean>, Serializable {
    private final Predicate<T> predicate;

    private PredicateFunction(Predicate<T> predicate) {
      this.predicate = checkNotNull(predicate);
    }

    @Override
    public Boolean apply(@Nullable T t) {
      return predicate.apply(t);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof PredicateFunction) {
        PredicateFunction<?> that = (PredicateFunction<?>) obj;
        return predicate.equals(that.predicate);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return predicate.hashCode();
    }

    @Override
    public String toString() {
      return "Functions.forPredicate(" + predicate + ")";
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns a function that ignores its input and always returns {@code value}.
   *
   * <p><b>Java 8 users:</b> use the lambda expression {@code o -> value} instead.
   *
   * @param value the constant value for the function to return
   * @return a function that always returns {@code value}
   */
  public static <E> Function<Object, E> constant(@Nullable E value) {
    return new ConstantFunction<E>(value);
  }

  private static class ConstantFunction<E> implements Function<Object, E>, Serializable {
    private final @Nullable E value;

    public ConstantFunction(@Nullable E value) {
      this.value = value;
    }

    @Override
    public E apply(@Nullable Object from) {
      return value;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof ConstantFunction) {
        ConstantFunction<?> that = (ConstantFunction<?>) obj;
        return Objects.equal(value, that.value);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return (value == null) ? 0 : value.hashCode();
    }

    @Override
    public String toString() {
      return "Functions.constant(" + value + ")";
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns a function that ignores its input and returns the result of {@code supplier.get()}.
   *
   * <p><b>Java 8 users:</b> use the lambda expression {@code o -> supplier.get()} instead.
   *
   * @since 10.0
   */
  public static <T> Function<Object, T> forSupplier(Supplier<T> supplier) {
    return new SupplierFunction<T>(supplier);
  }

  /** @see Functions#forSupplier */
  private static class SupplierFunction<T> implements Function<Object, T>, Serializable {

    private final Supplier<T> supplier;

    private SupplierFunction(Supplier<T> supplier) {
      this.supplier = checkNotNull(supplier);
    }

    @Override
    public T apply(@Nullable Object input) {
      return supplier.get();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof SupplierFunction) {
        SupplierFunction<?> that = (SupplierFunction<?>) obj;
        return this.supplier.equals(that.supplier);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return supplier.hashCode();
    }

    @Override
    public String toString() {
      return "Functions.forSupplier(" + supplier + ")";
    }

    private static final long serialVersionUID = 0;
  }
}
