package com.google.common.base;

import java.io.Serializable;

/**
 * Let java like scala use lazy freely
 */
public class LazyReference<T>
        implements Serializable
{
    private final Supplier<T> supplier;
    private transient volatile T instance;

    private LazyReference(Supplier<T> supplier)
    {
        this.supplier = supplier;
    }

    public static <T> LazyReference<T> goLazy(Supplier<T> supplier)
    {
        return new LazyReference<>(supplier);
    }

    public T get()
    {
        if (instance == null) {
            synchronized (supplier) {  //1
                if (instance == null) {          //2
                    instance = supplier.get();  //3
                }
            }
        }
        return instance;
    }

    @FunctionalInterface
    public interface Supplier<T>
            extends Serializable
    {
        T get();
    }
}
