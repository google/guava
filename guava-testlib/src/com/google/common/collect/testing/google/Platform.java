// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.common.collect.testing.google;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.lang.reflect.Method;

/**
 * Methods factored out so that they can be emulated in GWT.
 *
 * @author hhchan@google.com (Hayward Chan)
 */
@GwtCompatible
class Platform {

  @GwtIncompatible("Class.getMethod, java.lang.reflect.Method")
  static Method getMethod(Class<?> clazz, String name) {
    try {
      return clazz.getMethod(name);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
