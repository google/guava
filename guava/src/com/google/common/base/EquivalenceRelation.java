package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Binary relation that is at the same time a reflexive relation, a symmetric relation and a transitive relation
 *
 * In this implementation if you have relation (42, 0) and (null, 42), you will have a collision by a hashcode because
 * an implementation of a hashcode method says if(value == null) then value hashcode = 0 and if you use integer 0
 * then hashcode equals 0 too
 *
 * @author Ivan Osipov
 */
@Beta
@GwtCompatible
public final class EquivalenceRelation<T> implements Serializable {

    private static final long serialVersionUID = 0;

    private final T firstOne;

    private final T secondOne;

    private EquivalenceRelation(@Nullable T firstOne,@Nullable T secondOne) {
        this.firstOne = firstOne;
        this.secondOne = secondOne;
    }

    /**
     * Creates an equivalence relation between two same type objects and returns it
     *
     * @param firstOne can be null
     * @param secondOne can be null
     */
    @GwtCompatible
    public static <S> EquivalenceRelation of(@Nullable S firstOne,@Nullable S secondOne) {
        return new EquivalenceRelation<>(firstOne, secondOne);
    }

    public T getFirstOne() {
        return firstOne;
    }

    public T getSecondOne() {
        return secondOne;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EquivalenceRelation<?> anotherOne = (EquivalenceRelation<?>) o;

        return Objects.equal(firstOne, anotherOne.firstOne) ? Objects.equal(secondOne, anotherOne.secondOne)
                : (Objects.equal(firstOne, anotherOne.secondOne) && Objects.equal(secondOne, anotherOne.firstOne));
    }

    @Override
    public int hashCode() {
        int result = firstOne != null ? firstOne.hashCode() : 0;
        result += secondOne != null ? secondOne.hashCode() : 0;
        return result;
    }
}
