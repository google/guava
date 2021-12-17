/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.testing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@code TearDownStack} contains a stack of {@link TearDown} instances.
 *
 * <p>This class is thread-safe.
 *
 * @author Kevin Bourrillion
 * @since 10.0
 */
@Beta
@GwtCompatible
public class TearDownStack implements TearDownAccepter {
  private static final Logger logger = Logger.getLogger(TearDownStack.class.getName());

  @GuardedBy("stack")
  final LinkedList<TearDown> stack = new LinkedList<>();

  private final boolean suppressThrows;

  public TearDownStack() {
    this.suppressThrows = false;
  }

  public TearDownStack(boolean suppressThrows) {
    this.suppressThrows = suppressThrows;
  }

  @Override
  public final void addTearDown(TearDown tearDown) {
    synchronized (stack) {
      stack.addFirst(checkNotNull(tearDown));
    }
  }

  /** Causes teardown to execute. */
  public final void runTearDown() {
    List<Throwable> exceptions = new ArrayList<>();
    List<TearDown> stackCopy;
    synchronized (stack) {
      stackCopy = Lists.newArrayList(stack);
      stack.clear();
    }
    for (TearDown tearDown : stackCopy) {
      try {
        tearDown.tearDown();
      } catch (Throwable t) {
        if (suppressThrows) {
          logger.log(Level.INFO, "exception thrown during tearDown", t);
        } else {
          exceptions.add(t);
        }
      }
    }
    if (!suppressThrows && (exceptions.size() > 0)) {
      throw ClusterException.create(exceptions);
    }
  }
}
