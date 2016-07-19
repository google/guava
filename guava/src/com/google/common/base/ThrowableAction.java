package com.google.common.base;


/**
 * Action callback with possibility of exception throw.
 */
public interface ThrowableAction {

  /**
   * Calls action.
   *
   * @throws Throwable when exception occurs
   */
   void call() throws Throwable;
}
