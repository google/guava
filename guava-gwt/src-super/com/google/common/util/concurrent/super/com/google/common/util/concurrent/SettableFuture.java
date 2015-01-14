/*
 * Copyright (C) 2015 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.util.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Emulation for SettableFuture in GWT.
 */
public final class SettableFuture<V> implements ListenableFuture<V> {

  private static final Logger log = Logger.getLogger(SettableFuture.class.getName());

  public static <V> SettableFuture<V> create() {
    return new SettableFuture<V>();
  }
  
  private State state;
  private V value;
  private Throwable throwable;
  private List<Listener> listeners;
  
  private SettableFuture() {
    state = State.PENDING;
    listeners = new ArrayList<Listener>();
  }
  
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (!state.canTransitionTo(State.CANCELLED)) {
      return false;
    }
    
    state = State.CANCELLED;
    notifyAndClearListeners();
    return true;
  }

  @Override
  public boolean isCancelled() {
    return state.isCancelled();
  }

  @Override
  public boolean isDone() {
    return state.isDone();
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    state.maybeThrow(throwable);
    return value;
  }

  @Override
  public V get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return get();
  }

  @Override
  public void addListener(Runnable runnable, Executor executor) {
    if (isDone()) {
      executor.execute(runnable);
    } else {
      listeners.add(new Listener(runnable, executor));
    }
  }

  public boolean setException(Throwable throwable) {
    if (!state.canTransitionTo(State.FAILURE)) {
      return false;
    }
    
    this.throwable = throwable;
    this.state = State.FAILURE;
    notifyAndClearListeners();
    return true;
  }

  public boolean set(V value) {
    if (!state.canTransitionTo(State.VALUE)) {
      return false;
    }

    this.value = value;
    this.state = State.VALUE;
    notifyAndClearListeners();
    return true;
  }
  
  private void notifyAndClearListeners() {
    for (Listener listener : listeners) {
      listener.execute();
    }
    listeners = null;
  }

  private enum State {
    CANCELLED {
      @Override
      boolean isCancelled() {
        return true;
      }
      
      @Override
      void maybeThrow(Throwable cause) throws ExecutionException {
        throw new CancellationException();
      }
    },
    PENDING {
      @Override
      boolean isDone() {
        return false;
      }
      
      @Override
      void maybeThrow(Throwable cause) throws ExecutionException {
        throw new IllegalStateException("Cannot get() on a pending future.");
      }
      
      @Override
      boolean canTransitionTo(State state) {
        return !state.equals(PENDING);
      }
    },
    VALUE,
    FAILURE {
      @Override
      void maybeThrow(Throwable cause) throws ExecutionException {
        throw new ExecutionException(cause);
      }
    };
    
    boolean isDone() {
      return true;
    }
    
    boolean isCancelled() {
      return false;
    }

    void maybeThrow(Throwable cause) throws ExecutionException {}
    
    boolean canTransitionTo(State state) {
      return false;
    }
  }

  private static class Listener {
    final Runnable command;
    final Executor executor;
    
    Listener(Runnable command, Executor executor) {
      this.command = command;
      this.executor = executor;
    }
    
    void execute() {
      try {
        executor.execute(command);
      } catch (RuntimeException e) {
        log.log(Level.SEVERE, "RuntimeException while executing runnable "
            + command + " with executor " + executor, e);

      }
    }
  }
}
