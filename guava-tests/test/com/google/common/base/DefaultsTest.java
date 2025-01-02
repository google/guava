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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtIncompatible;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit test for {@link Defaults}.
 *
 * @author Jige Yu
 */
@GwtIncompatible
@NullUnmarked
public class DefaultsTest extends TestCase {
  public void testGetDefaultValue() {
    assertEquals(false, Defaults.defaultValue(boolean.class).booleanValue());
    assertEquals('\0', Defaults.defaultValue(char.class).charValue());
    assertEquals(0, Defaults.defaultValue(byte.class).byteValue());
    assertEquals(0, Defaults.defaultValue(short.class).shortValue());
    assertEquals(0, Defaults.defaultValue(int.class).intValue());
    assertEquals(0, Defaults.defaultValue(long.class).longValue());
    assertEquals(0.0f, Defaults.defaultValue(float.class).floatValue());
    assertThat(Defaults.defaultValue(double.class).doubleValue()).isEqualTo(0.0d);
    assertNull(Defaults.defaultValue(void.class));
    assertNull(Defaults.defaultValue(String.class));
  }
}
