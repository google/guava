/*
 * Copyright (C) 2014 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.NullPointerTester;
import junit.framework.TestCase;

/** Tests for {@link MoreObjects}. */
@GwtCompatible(emulated = true)
public class MoreObjectsTest extends TestCase {
  public void testFirstNonNull_withNonNull() {
    String s1 = "foo";
    String s2 = MoreObjects.firstNonNull(s1, "bar");
    assertSame(s1, s2);

    Long n1 = 42L;
    Long n2 = MoreObjects.firstNonNull(null, n1);
    assertSame(n1, n2);

    Boolean b1 = true;
    Boolean b2 = MoreObjects.firstNonNull(b1, null);
    assertSame(b1, b2);
  }

  public void testFirstNonNull_throwsNullPointerException() {
    try {
      MoreObjects.firstNonNull(null, null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  // ToStringHelper's tests are in ToStringHelperTest

  @J2ktIncompatible
  @GwtIncompatible("NullPointerTester")
  public void testNulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.ignore(MoreObjects.class.getMethod("firstNonNull", Object.class, Object.class));
    tester.testAllPublicStaticMethods(MoreObjects.class);
    tester.testAllPublicInstanceMethods(MoreObjects.toStringHelper(new TestClass()));
  }

  /** Test class for testing formatting of inner classes. */
  private static class TestClass {}
}
