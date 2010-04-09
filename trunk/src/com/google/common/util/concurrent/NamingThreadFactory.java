/*
 * Copyright (C) 2006 Google Inc.
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

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A ThreadFactory which decorates another ThreadFactory to set a name on
 * each thread created.
 *
 * @author Kevin Bourrillion
 * @since 2009.09.15 <b>tentative</b>
 */
public class NamingThreadFactory implements ThreadFactory {
  private final ThreadFactory backingFactory;
  private final String format;
  private final AtomicInteger count = new AtomicInteger(0);

  public static final ThreadFactory DEFAULT_FACTORY
      = Executors.defaultThreadFactory();

  /**
   * Creates a new factory that delegates to the default thread factory for
   * thread creation, then uses {@code format} to construct a name for the new
   * thread.
   *
   * @param format a {@link String#format(String, Object...)}-compatible format
   *     String, to which a unique integer (0, 1, etc.) will be supplied as the
   *     single parameter. This integer will be unique to this instance of
   *     NamingThreadFactory and will be assigned sequentially.
   */
  public NamingThreadFactory(String format) {
    this(format, DEFAULT_FACTORY);
  }

  /**
   * Creates a new factory that delegates to {@code backingFactory} for thread
   * creation, then uses {@code format} to construct a name for the new thread.
   *
   * @param format a {@link String#format(String, Object...)}-compatible format
   *     String, to which a unique integer (0, 1, etc.) will be supplied as the
   *     single parameter
   * @param backingFactory the factory that will actually create the threads
   * @throws java.util.IllegalFormatException if {@code format} is invalid
   */
  public NamingThreadFactory(String format, ThreadFactory backingFactory) {
    this.format = format;
    this.backingFactory = backingFactory;
    makeName(0); // fail fast if format is bad
  }

  public Thread newThread(Runnable r) {
    Thread t = backingFactory.newThread(r);
    t.setName(makeName(count.getAndIncrement()));
    return t;
  }

  private String makeName(int ordinal) {
    return String.format(format, ordinal);
  }
}
