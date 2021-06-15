package com.google.common.base;

import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents an operation that accepts two input arguments and returns no
 * result.  This is the two-arity specialization of {@link java.util.function.Consumer}.
 * Unlike most other functional interfaces, {@code TriConsumer} is expected
 * to operate via side-effects.
 *
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 * @param <V> the type of the third argument to the operation
 *
 * @see java.util.function.Consumer
 * @see java.util.function.BiConsumer
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     * @param v the second input argument
     */
    void accept(@Nullable T t, @Nullable U u, @Nullable V v);

    /**
     * Returns a composed {@code TriConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code TriConsumer} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default TriConsumer<T, U, V> andThen(TriConsumer<? super T, ? super U, ? super V> after) {
        checkNotNull(after);

        return (t, u, v) -> {
            accept(t, u, v);
            after.accept(t, u, v);
        };
    }
}
