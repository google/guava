/*
 * Copyright (C) 2016 The Guava Authors
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

package com.google.common.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A class to represent the set of edges connecting an (implicit) origin node to a target node.
 *
 * <p>The {@link #nodeToEdgeMap} means this class only works on networks without parallel edges.
 *
 * @author James Sexton
 * @param <E> Edge parameter type
 */
final class SimpleEdgesConnecting<E> extends AbstractSet<E> {

  private final Map<?, E> nodeToEdgeMap;
  private final Object targetNode;

  SimpleEdgesConnecting(Map<?, E> nodeToEdgeMap, Object targetNode) {
    this.nodeToEdgeMap = checkNotNull(nodeToEdgeMap);
    this.targetNode = checkNotNull(targetNode);
  }

  @Override
  public Iterator<E> iterator() {
    E connectingEdge = getConnectingEdge();
    return (connectingEdge == null)
        ? ImmutableSet.<E>of().iterator()
        : Iterators.singletonIterator(connectingEdge);
  }

  @Override
  public int size() {
    return getConnectingEdge() == null ? 0 : 1;
  }

  @Override
  public boolean contains(@Nullable Object obj) {
    E connectingEdge = getConnectingEdge();
    return (connectingEdge != null && connectingEdge.equals(obj));
  }

  @Nullable private E getConnectingEdge() {
    return nodeToEdgeMap.get(targetNode);
  }
}
