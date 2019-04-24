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

package com.google.common.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.PatternSyntaxException;
import junit.framework.TestCase;

/**
 * Unit test for {@link PatternFilenameFilter}.
 *
 * @author Chris Nokleberg
 */
public class PatternFilenameFilterTest extends TestCase {

  public void testSyntaxException() {
    try {
      new PatternFilenameFilter("(");
      fail("expected exception");
    } catch (PatternSyntaxException expected) {
    }
  }

  public void testAccept() {
    File dir = new File("foo");
    FilenameFilter filter = new PatternFilenameFilter("a+");
    assertTrue(filter.accept(dir, "a"));
    assertTrue(filter.accept(dir, "aaaa"));
    assertFalse(filter.accept(dir, "b"));

    // Show that dir is ignored
    assertTrue(filter.accept(null, "a"));
  }
}
