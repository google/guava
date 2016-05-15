package com.google.common.base;

/**
 * Static utility methofs for {@link ThrowableAction} interface.
 */
public final class ThrowableActions {
  private ThrowableActions() {}

  private static final ThrowableAction EMPTY_ACTION =
      new ThrowableAction() {
        @Override
        public void call() throws Throwable {}
      };

   /**
    * Returns empty action.
    */
   public static ThrowableAction doNothing() {
	   return EMPTY_ACTION;
   }
}
