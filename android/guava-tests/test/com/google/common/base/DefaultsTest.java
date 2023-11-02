/*
 * Copyright (C) 2007 The Guava Authors
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

import com.google.common.annotations.GwtIncompatible;
import junit.framework.TestCase;

/**
 * Unit test for {@link Defaults}.
 *
 * @author Jige Yu
 */
@GwtIncompatible
public class DefaultsTest extends TestCase {
  public void testGetDefaultValue() {
    assertEquals(false, Defaults.defaultValue(boolean.class).booleanValue());
    assertEquals('\0', Defaults.defaultValue(char.class).charValue());
    assertEquals(0, Defaults.defaultValue(byte.class).byteValue());
    assertEquals(0, Defaults.defaultValue(short.class).shortValue());
    assertEquals(0, Defaults.defaultValue(int.class).intValue());
    assertEquals(0, Defaults.defaultValue(long.class).longValue());
    assertEquals(0.0f, Defaults.defaultValue(float.class).floatValue());
    assertEquals(0.0d, Defaults.defaultValue(double.class).doubleValue());
    assertNull(Defaults.defaultValue(void.class));
    assertNull(Defaults.defaultValue(String.class));
  }
}
