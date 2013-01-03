/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.base;

import junit.framework.TestCase;

import java.lang.reflect.Field;

/**
 * Tests for {@link StandardSystemProperties}.
 *
 * @author Kurt Alfred Kluever
 */
public class StandardSystemPropertiesTest extends TestCase {

  public void testConstantNameMatchesString() throws Exception {
    for (Field field : StandardSystemProperties.class.getFields()) {
      String actual = (String) field.get(null);
      String expected = Ascii.toLowerCase(field.getName()).replaceAll("_", ".");
      assertEquals(expected, actual);
    }
  }

  // TODO(user): Consider checking to make sure the property isn't null.
}
