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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.MapConstraintsTest.TestKeyException;
import com.google.common.collect.MapConstraintsTest.TestValueException;

/**
 * Tests for {@link MapConstraints#constrainedBiMap}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class ConstrainedBiMapTest extends AbstractBiMapTest {

  private static final Integer TEST_KEY = 42;
  private static final String TEST_VALUE = "test";
  private static final MapConstraint<Integer, String> TEST_CONSTRAINT
      = new TestConstraint();

  private static final class TestConstraint
      implements MapConstraint<Integer, String> {
    @Override
    public void checkKeyValue(Integer key, String value) {
      if (TEST_KEY.equals(key)) {
        throw new TestKeyException();
      }
      if (TEST_VALUE.equals(value)) {
        throw new TestValueException();
      }
    }
    private static final long serialVersionUID = 0;
  }

  @Override protected BiMap<Integer, String> create() {
    return MapConstraints.constrainedBiMap(
        HashBiMap.<Integer, String>create(), TEST_CONSTRAINT);
  }

  // not serializable
  @GwtIncompatible("SerializableTester")
  @Override
  public void testSerialization() {}
  
  @GwtIncompatible("SerializableTester")
  @Override
  public void testSerializationWithInverseEqual() {}
  
  @GwtIncompatible("SerializableTester")
  @Override
  public void testSerializationWithInverseSame() {}
}
