/*
 * Copyright (C) 2010 The Guava Authors
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.NullPointerTester;
import java.util.concurrent.atomic.AtomicReferenceArray;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit test for {@link Atomics}.
 *
 * @author Kurt Alfred Kluever
 */
@NullUnmarked
@GwtIncompatible
public class AtomicsTest extends TestCase {

  private static final Object OBJECT = new Object();

  public void testNewReference() {
    assertThat(Atomics.newReference().get()).isNull();
  }

  public void testNewReference_withInitialValue() {
    assertThat(Atomics.newReference(null).get()).isNull();
    assertEquals(OBJECT, Atomics.newReference(OBJECT).get());
  }

  public void testNewReferenceArray_withLength() {
    int length = 42;
    AtomicReferenceArray<String> refArray = Atomics.newReferenceArray(length);
    for (int i = 0; i < length; ++i) {
      assertThat(refArray.get(i)).isNull();
    }
    assertThrows(IndexOutOfBoundsException.class, () -> refArray.get(length));
  }

  @J2ktIncompatible // J2KT throws IllegalArgumentException instead of NegativeArraySizeException
  public void testNewReferenceArray_withNegativeLength() {
    assertThrows(NegativeArraySizeException.class, () -> Atomics.newReferenceArray(-1));
  }

  public void testNewReferenceArray_withStringArray() {
    String[] array = {"foo", "bar", "baz"};
    AtomicReferenceArray<String> refArray = Atomics.newReferenceArray(array);
    for (int i = 0; i < array.length; ++i) {
      assertThat(refArray.get(i)).isEqualTo(array[i]);
    }
    assertThrows(IndexOutOfBoundsException.class, () -> refArray.get(array.length));
  }

  public void testNewReferenceArray_withNullArray() {
    assertThrows(NullPointerException.class, () -> Atomics.newReferenceArray((String[]) null));
  }

  @J2ktIncompatible
  public void testNullPointers() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicConstructors(Atomics.class); // there aren't any
    tester.testAllPublicStaticMethods(Atomics.class);
  }
}
