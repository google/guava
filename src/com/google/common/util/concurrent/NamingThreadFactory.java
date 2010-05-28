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

import com.google.common.annotations.Beta;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * A ThreadFactory which decorates another ThreadFactory to set a name on
 * each thread created.
 *
 * @author Kevin Bourrillion
 * @since 1
 * @deprecated Create a {@link ThreadFactoryBuilder} and then use its
 *     {@link ThreadFactoryBuilder#setNameFormat} and
 *     {@link ThreadFactoryBuilder#setThreadFactory} methods.
 */
@Beta
@Deprecated
public final class NamingThreadFactory implements ThreadFactory {
  private final ThreadFactory delegate;

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
    this.delegate = new ThreadFactoryBuilder()
        .setNameFormat(format)
        .setThreadFactory(Executors.defaultThreadFactory())
        .build();
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
    this.delegate = new ThreadFactoryBuilder()
        .setNameFormat(format)
        .setThreadFactory(backingFactory)
        .build();
  }

  public Thread newThread(Runnable r) {
    return delegate.newThread(r);
  }
}
