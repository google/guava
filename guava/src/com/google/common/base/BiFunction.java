package com.google.common.base;

/**
 * Imitate java8 implementation of BiFunction.
 * Represents a function that accepts two arguments and produces a result.
 * Created by lizhibo on 2017/8/12.
 */
public interface BiFunction<T, U, R> {

    R apply(T t,U u);
}
