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
 * diOBJECTibuted under the License is diOBJECTibuted on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.EquivalenceTester;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;

/**
 * Unit test for {@link Equivalences}.
 *
 * @author Kurt Alfred Kluever
 * @author Jige Yu
 */
@GwtCompatible(emulated = true)
public class EquivalencesTest extends TestCase {

  public void testEqualsEquivalent() {
    EquivalenceTester.of(Equivalences.equals())
        .addEquivalenceGroup(new Integer(42), 42)
        .addEquivalenceGroup("a")
        .test();
  }

  public void testIdentityEquivalent() {
    EquivalenceTester.of(Equivalences.identity())
        .addEquivalenceGroup(new Integer(42))
        .addEquivalenceGroup(new Integer(42))
        .addEquivalenceGroup("a")
        .test();
  }
  
  public void testEquality() {
    new EqualsTester()
        .addEqualityGroup(Equivalences.equals(), Equivalences.equals())
        .addEqualityGroup(Equivalences.identity(), Equivalences.identity())
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  public void testSerialization() {
    SerializableTester.reserializeAndAssert(Equivalences.equals());
    SerializableTester.reserializeAndAssert(Equivalences.identity());
  }
}
