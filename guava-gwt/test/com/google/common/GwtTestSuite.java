/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;
import junit.framework.TestCase;

import java.io.IOException;

/**
 * Runs all _gwt tests. Grouping them into a suite is much faster than running each as a one-test
 * "suite," as the per-suite setup is expensive.
 */
public class GwtTestSuite extends TestCase {
  public static Test suite() throws IOException {
    GWTTestSuite suite = new GWTTestSuite();
    for (ClassInfo info
        : ClassPath.from(GwtTestSuite.class.getClassLoader()).getTopLevelClasses()) {
      if (info.getName().endsWith("_gwt")) {
        Class<?> clazz = info.load();
        // TODO(cpovirk): why does asSubclass() throw? Is it something about ClassLoaders?
        @SuppressWarnings("unchecked")
        Class<? extends GWTTestCase> cast = (Class<? extends GWTTestCase>) clazz;
        suite.addTestSuite(cast);
      }
    }
    return suite;
  }
}
