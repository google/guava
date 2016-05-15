package com.google.common.base;

import com.google.common.annotations.GwtCompatible;

/**
 * Action callback with possibility of exception throw.
 */
@GwtCompatible
public interface ThrowableAction {

  /**
   * Calls action.
   *
   * @throws Throwable when exception occurs
   */
   void call() throws Throwable;
}
