/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.BstTesting.SimpleNode;

import junit.framework.TestCase;

/**
 * Simple tests for {@code BstPath}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class BstPathTest extends TestCase {
  static class SimplePath extends BstPath<SimpleNode, SimplePath> {
    private SimplePath(SimpleNode tip, SimplePath tail) {
      super(tip, tail);
    }
  }

  public void testTailAtRoot() {
    SimpleNode root = new SimpleNode('a', null, null);
    SimplePath rootPath = new SimplePath(root, null);
    assertFalse(rootPath.hasPrefix());
    assertNull(rootPath.prefixOrNull());
    try {
      rootPath.getPrefix();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {}
  }

  public void testTailDown() {
    SimpleNode node = new SimpleNode('a', null, null);
    SimpleNode root = new SimpleNode('b', node, null);
    SimplePath rootPath = new SimplePath(root, null);
    SimplePath nodePath = new SimplePath(node, rootPath);
    assertTrue(nodePath.hasPrefix());
    assertEquals(rootPath, nodePath.prefixOrNull());
    assertEquals(rootPath, nodePath.getPrefix());
  }
}
