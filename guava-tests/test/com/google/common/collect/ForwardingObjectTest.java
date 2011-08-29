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

package com.google.common.collect;

import junit.framework.TestCase;

import java.util.Set;

/**
 * Tests for {@code ForwardingObject}.
 *
 * @author Mike Bostock
 */
public class ForwardingObjectTest extends TestCase {

  public void testEqualsReflexive() {
    final Object delegate = new Object();
    ForwardingObject forward = new ForwardingObject() {
      @Override protected Object delegate() {
        return delegate;
      }
    };
    assertTrue(forward.equals(forward));
  }

  public void testEqualsSymmetric() {
    final Set<String> delegate = Sets.newHashSet("foo");
    ForwardingObject forward = new ForwardingObject() {
      @Override protected Object delegate() {
        return delegate;
      }
    };
    assertEquals(forward.equals(delegate), delegate.equals(forward));
  }
}
