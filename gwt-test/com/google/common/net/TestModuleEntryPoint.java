// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.common.net;

import com.google.gwt.core.client.EntryPoint;

import java.util.Arrays;
import java.util.List;

/**
 * @author hhchan@google.com (Hayward Chan)
 */
public class TestModuleEntryPoint implements EntryPoint {

  // It matches all classes that are both @GwtCompatible and Google internal.

  @Override
  public void onModuleLoad() {
    // TODO: Auto generate this list.
    List<Class<?>> allClasses = Arrays.<Class<?>>asList(
        InternetDomainName.class,
        TldPatterns.class);
  }
}
