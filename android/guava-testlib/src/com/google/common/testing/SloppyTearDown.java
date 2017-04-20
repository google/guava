/*
 * Copyright (C) 2009 The Guava Authors
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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple utility for when you want to create a {@link TearDown} that may throw an exception but
 * should not fail a test when it does. (The behavior of a {@code TearDown} that throws an exception
 * varies; see its documentation for details.) Use it just like a {@code TearDown}, except override
 * {@link #sloppyTearDown()} instead.
 *
 * @author Luiz-Otavio Zorzella
 * @since 10.0
 */
@Beta
@GwtCompatible
public abstract class SloppyTearDown implements TearDown {
  private static final Logger logger = Logger.getLogger(SloppyTearDown.class.getName());

  @Override
  public final void tearDown() {
    try {
      sloppyTearDown();
    } catch (Throwable t) {
      logger.log(Level.INFO, "exception thrown during tearDown: " + t.getMessage(), t);
    }
  }

  public abstract void sloppyTearDown() throws Exception;
}
