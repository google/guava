// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.common.collect.testing.testers;

import java.lang.reflect.Method;

/**
 * This class is emulated in GWT.
 *
 * @author hhchan@google.com (Hayward Chan)
 */
class Platform {

  /**
   * Delegate to {@link Class#getMethod(String, Class[])}.  Not
   * usable in GWT.
   */
  static Method getMethod(Class<?> clazz, String methodName) {
    try {
      return clazz.getMethod(methodName);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Format the template with args, only supports the placeholder
   * {@code %s}.
   */
  static String format(String template, Object... args) {
    return String.format(template, args);
  }

  /** See {@link ListListIteratorTester} */
  static int listListIteratorTesterNumIterations() {
    return 4;
  }

  /** See {@link CollectionIteratorTester} */
  static int collectionIteratorTesterNumIterations() {
    return 5;
  }
}
