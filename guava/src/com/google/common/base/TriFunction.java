package com.google.common.base;

import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a function that accepts two arguments and produces a result.
 * This is the two-arity specialization of {@link Function}.
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <V> the type of the third argument to the function
 * @param <R> the type of the result of the function
 *
 * @see Function
 * @see java.util.function.BiFunction
 */
@FunctionalInterface
public interface TriFunction<T, U, V, R> {

    /**
     * Applies this function to the given arguments.
     *
     * @param t the first function argument
     * @param u the second function argument
     * @param v the third function argument
     * @return the function result
     */
    @Nullable
    R apply(@Nullable T t, @Nullable U u, @Nullable V v);

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <S> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <S> TriFunction<T, U, V, S> andThen(Function<? super R, ? extends S> after) {
        checkNotNull(after);
        return (T t, U u, V v) -> after.apply(apply(t, u, v));
    }
}
