/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkState;

/**
 * Note this class is a copy of {@link com.google.common.collect.AbstractIterator} (for dependency
 * reasons).
 */
@GwtCompatible
abstract class AbstractIterator<T extends @Nullable Object> implements Iterator<T> {
    private State state = State.NOT_READY;
    private @Nullable T next;

    protected AbstractIterator() {
    }

    protected abstract @Nullable T computeNext();

    @CanIgnoreReturnValue
    protected final @Nullable T endOfData() {
        state = State.DONE;
        return null;
    }

    @Override
    public final boolean hasNext() {
        checkState(state != State.FAILED);
        switch (state) {
            case DONE:
                return false;
            case READY:
                return true;
            default:
                return tryToComputeNext();
        }
    }

    private boolean tryToComputeNext() {
        state = State.FAILED;
        next = computeNext();

        if (state == State.DONE) {
            // endOfData() was called during computeNext()
            return false;
        }

        if (next == null) {
            // computeNext() returned null but didn't call endOfData()
            // This is either an error or should be treated as terminal state
            state = State.DONE;
            return false;
        }

        state = State.READY;
        return true;
    }

    @Override
    @ParametricNullness
    public final T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        state = State.NOT_READY;
        @SuppressWarnings("nullness") // hasNext() ensures next is non-null
        T result = next;
        next = null;
        return result;
    }

    @Override
    public final void remove() {
        throw new UnsupportedOperationException();
    }


    private enum State {
        READY, NOT_READY, DONE, FAILED
    }
}
