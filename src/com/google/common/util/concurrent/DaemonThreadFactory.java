/*
 * Copyright (C) 2008 Google Inc.
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

import com.google.common.base.Preconditions;

import java.util.concurrent.ThreadFactory;

/**
 * Wraps another {@link ThreadFactory}, making all new threads daemon threads.
 *
 * @author Charles Fry
 * @author Harendra Verma
 * @since 2009.09.15 <b>tentative</b>
 */
public class DaemonThreadFactory implements ThreadFactory {

  private final ThreadFactory factory;

  public DaemonThreadFactory(ThreadFactory factory) {
    Preconditions.checkNotNull(factory);
    this.factory = factory;
  }

  public Thread newThread(Runnable r) {
    Thread t = factory.newThread(r);
    t.setDaemon(true);
    return t;
  }
}
