// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.common.reflect;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Captures the actual type of {@code T}.
 *
 * @author benyu@google.com (Ben Yu)
 */
abstract class TypeCapture<T> {

  /** Returns the captured type. */
  final Type capture() {
    Type superclass = getClass().getGenericSuperclass();
    checkArgument(superclass instanceof ParameterizedType,
        "%s isn't parameterized", superclass);
    return ((ParameterizedType) superclass).getActualTypeArguments()[0];
  }
}
