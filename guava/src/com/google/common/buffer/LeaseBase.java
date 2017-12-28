package com.google.common.buffer;

import com.google.common.annotations.GwtCompatible;

@GwtCompatible
public abstract class LeaseBase<T> implements Lease<T> {

    protected final T buffer;
    private boolean isInUse;

    public LeaseBase(T buffer) {
        this.buffer = buffer;
    }

    @Override
    public T getBuffer() {
        isInUse = true;
        return buffer;
    }

    @Override
    public void returnLease() {
        isInUse = false;
    }

    @Override
    public boolean isInUse() {
        return isInUse;
    }
}
