// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.common.primitives;

import com.google.gwt.core.client.EntryPoint;

import java.util.Arrays;
import java.util.List;

/**
 * @author hhchan@google.com (Hayward Chan)
 */
public class TestModuleEntryPoint implements EntryPoint {

  @Override
  public void onModuleLoad() {
    // TODO: Auto generate this list.
    List<Class<?>> allClasses = Arrays.<Class<?>>asList(
        Booleans.class,
        Ints.class,
        Shorts.class,
        Chars.class,
        Longs.class,
        Floats.class,
        Doubles.class,
        Bytes.class,
        SignedBytes.class);
  }
}
