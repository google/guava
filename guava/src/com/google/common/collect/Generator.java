package com.google.common.collect;

/**
 * Interface for implementation of Generator code that can yield() elements to the environment
 * (i.e. the Iterator calling the Generator. Generators are presented to you program code wrapped
 * in an UnmodifiableIterator that calls the Generator to generate new value as the iterator is iterated.
 *
 * @author  Eirik Maus
 * @since 19.0
 */
public interface Generator<T, Y extends YieldTarget<T>> {

    /**
     * Produce new value(s) for the iterator to iterate over.
     * This method gets called when ever someone calls the surrounding Iterator's next() or hasNext() methods,
     * and all the previously yielded values have been consumed.
     * If no values are yielded, the Iterators hasNext()-method will return false and the
     * generator is finished. More than one value may be yielded. These will be kept in memory
     * until they are consumed by the iterator.
     *
     * All code executes in the same thread as the surrounding iterator calling this method.
     * Infinite generators must can not loop forever, but must return now and then.
     * They will be called again in order to proceed, once the previously yielded elements are consumed.
     *
     * @param yieldTarget - the target containing the yield(t) method that must be called to yield values.
     */
    void yieldNextValues(Y yieldTarget);
}
