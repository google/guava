// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.common.base;

import com.google.gwt.core.client.EntryPoint;

import java.util.Arrays;
import java.util.List;

/**
 * A dummy entry point that accesses all GWT classes in
 * {@code com.google.common.base}.
 *
 * @author hhchan@google.com (Hayward Chan)
 */
@SuppressWarnings("unchecked")
public class TestModuleEntryPoint implements EntryPoint {

  // It matches all classes that are both @GwtCompatible and Google internal.

  @Override
  public void onModuleLoad() {
    // TODO: Auto generate this list.
    List<Class<?>> allClasses = Arrays.<Class<?>>asList(
        CharMatcher.class,
        Function.class,
        Functions.class,
        Joiner.class,
        Objects.class,
        Preconditions.class,
        Predicate.class,
        Predicates.class,
        Strings.class,
        Splitter.class,
        Supplier.class);
  }
}
