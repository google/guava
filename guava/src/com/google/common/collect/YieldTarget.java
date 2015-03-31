package com.google.common.collect;

/**
 * Interface that Generators call to yield new values that can be iterated over (or whatever).
 * Only used inside the generator to output a value item.
 *
 * @author Eirik Maus
 * @since 19.0
 */
public interface YieldTarget<T> {
    void yield(T element);
}
