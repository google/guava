package com.google.common.base;

/**
 * When the toMap occurs when the key conflict,
 * will call this interface to deal with the conflict,
 * the interface receives two values, the old value and the new value,
 * return the value after the combination of key
 * Created by lizhibo on 2017/8/12.
 */
public interface MergeFunction<V> extends BiFunction<V,V,V>  {

    /**
     *
     * @param oldValue
     * @param newValue
     * @return The combined value
     */
    V apply(V oldValue,V newValue);
}
