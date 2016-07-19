package com.google.common.collect;

/**
 * Extended interface for YieldTargets that can keep some state between invocations
 * of the generator code, on behalf of the Generator. @see Generator .
 *
 * @author Eirik Maus
 * @since 19.0
 */
public interface StatefulYieldTarget<S, T> extends YieldTarget<T> {

    /** returns the last element yielded */
    T previous();

    /**
     * Set some state that the Generator might want to keep between calls.
     * @param state
     */
    void setState(S state);

    /**
     * Get the state that was saved using setState(S).
     */
    S getState();

}
