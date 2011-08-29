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

import static com.google.common.collect.BstSide.LEFT;
import static com.google.common.collect.BstSide.RIGHT;
import static com.google.common.collect.BstTesting.defaultNullPointerTester;
import static com.google.common.collect.BstTesting.extension;
import static com.google.common.collect.BstTesting.pathToList;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.BstTesting.SimpleNode;

import junit.framework.TestCase;

/**
 * Tests for {@code BstInOrderPath}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class BstInOrderPathTest extends TestCase {
  public void testFullTreeRight() {
    //    d
    //   / \
    //  b   f
    // / \ / \
    // a c e g
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', a, c);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', e, g);
    SimpleNode d = new SimpleNode('d', b, f);
    BstPathFactory<SimpleNode, BstInOrderPath<SimpleNode>> factory =
        BstInOrderPath.inOrderFactory();
    BstInOrderPath<SimpleNode> path = extension(factory, d, LEFT, LEFT);
    ASSERT.that(pathToList(path)).hasContentsInOrder(a, b, d);
    path = testNextPathIs(path, b, d);
    path = testNextPathIs(path, c, b, d);
    path = testNextPathIs(path, d);
    path = testNextPathIs(path, e, f, d);
    path = testNextPathIs(path, f, d);
    path = testNextPathIs(path, g, f, d);
    assertFalse(path.hasNext(RIGHT));
  }

  public void testFullTreeLeft() {
    //    d
    //   / \
    //  b   f
    // / \ / \
    // a c e g
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', a, c);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', e, g);
    SimpleNode d = new SimpleNode('d', b, f);
    BstPathFactory<SimpleNode, BstInOrderPath<SimpleNode>> factory =
        BstInOrderPath.inOrderFactory();
    BstInOrderPath<SimpleNode> path = extension(factory, d, RIGHT, RIGHT);
    ASSERT.that(pathToList(path)).hasContentsInOrder(g, f, d);
    path = testPrevPathIs(path, f, d);
    path = testPrevPathIs(path, e, f, d);
    path = testPrevPathIs(path, d);
    path = testPrevPathIs(path, c, b, d);
    path = testPrevPathIs(path, b, d);
    path = testPrevPathIs(path, a, b, d);
    assertFalse(path.hasNext(LEFT));
  }

  public void testPartialTree1Right() {

    //    d
    //   / \
    //  b   f
    // /     \
    // a     g
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode b = new SimpleNode('b', a, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', null, g);
    SimpleNode d = new SimpleNode('d', b, f);
    BstPathFactory<SimpleNode, BstInOrderPath<SimpleNode>> factory =
        BstInOrderPath.inOrderFactory();
    BstInOrderPath<SimpleNode> path = extension(factory, d, LEFT, LEFT);
    ASSERT.that(pathToList(path)).hasContentsInOrder(a, b, d);
    path = testNextPathIs(path, b, d);
    path = testNextPathIs(path, d);
    path = testNextPathIs(path, f, d);
    path = testNextPathIs(path, g, f, d);
    assertFalse(path.hasNext(RIGHT));
  }

  public void testPartialTree1Left() {

    //    d
    //   / \
    //  b   f
    // /     \
    // a     g
    SimpleNode a = new SimpleNode('a', null, null);
    SimpleNode b = new SimpleNode('b', a, null);
    SimpleNode g = new SimpleNode('g', null, null);
    SimpleNode f = new SimpleNode('f', null, g);
    SimpleNode d = new SimpleNode('d', b, f);
    BstPathFactory<SimpleNode, BstInOrderPath<SimpleNode>> factory =
        BstInOrderPath.inOrderFactory();
    BstInOrderPath<SimpleNode> path = extension(factory, d, RIGHT, RIGHT);
    ASSERT.that(pathToList(path)).hasContentsInOrder(g, f, d);
    path = testPrevPathIs(path, f, d);
    path = testPrevPathIs(path, d);
    path = testPrevPathIs(path, b, d);
    path = testPrevPathIs(path, a, b, d);
    assertFalse(path.hasNext(LEFT));
  }

  public void testPartialTree2Right() {
    //    d
    //   / \
    //  b   f
    //   \ /
    //   c e  
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', null, c);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode f = new SimpleNode('f', e, null);
    SimpleNode d = new SimpleNode('d', b, f);
    BstPathFactory<SimpleNode, BstInOrderPath<SimpleNode>> factory =
        BstInOrderPath.inOrderFactory();
    BstInOrderPath<SimpleNode> path = extension(factory, d, LEFT);
    ASSERT.that(pathToList(path)).hasContentsInOrder(b, d);
    path = testNextPathIs(path, c, b, d);
    path = testNextPathIs(path, d);
    path = testNextPathIs(path, e, f, d);
    path = testNextPathIs(path, f, d);
    assertFalse(path.hasNext(RIGHT));
  }

  public void testPartialTree2Left() {
    //    d
    //   / \
    //  b   f
    //   \ /
    //   c e  
    SimpleNode c = new SimpleNode('c', null, null);
    SimpleNode b = new SimpleNode('b', null, c);
    SimpleNode e = new SimpleNode('e', null, null);
    SimpleNode f = new SimpleNode('f', e, null);
    SimpleNode d = new SimpleNode('d', b, f);
    BstPathFactory<SimpleNode, BstInOrderPath<SimpleNode>> factory =
        BstInOrderPath.inOrderFactory();
    BstInOrderPath<SimpleNode> path = extension(factory, d, RIGHT);
    ASSERT.that(pathToList(path)).hasContentsInOrder(f, d);
    path = testPrevPathIs(path, e,f,d);
    path = testPrevPathIs(path, d);
    path = testPrevPathIs(path, c,b, d);
    path = testPrevPathIs(path, b, d);
    assertFalse(path.hasNext(LEFT));
  }

  private static BstInOrderPath<SimpleNode> testNextPathIs(
      BstInOrderPath<SimpleNode> path, SimpleNode... nodes) {
    assertTrue(path.hasNext(RIGHT));
    path = path.next(RIGHT);
    ASSERT.that(pathToList(path)).hasContentsInOrder(nodes);
    return path;
  }

  private static BstInOrderPath<SimpleNode> testPrevPathIs(
      BstInOrderPath<SimpleNode> path, SimpleNode... nodes) {
    assertTrue(path.hasNext(LEFT));
    path = path.next(LEFT);
    ASSERT.that(pathToList(path)).hasContentsInOrder(nodes);
    return path;
  }

  @GwtIncompatible("NullPointerTester")
  public void testNullPointers() throws Exception {
    defaultNullPointerTester().testAllPublicStaticMethods(BstInOrderPath.class);
  }
}
