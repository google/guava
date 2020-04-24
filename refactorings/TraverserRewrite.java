/*
 * Copyright (C) 2017 The Guava Authors
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

import com.google.common.collect.TreeTraverser;
import com.google.common.graph.Traverser;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.Placeholder;

/**
 * Refaster rules to rewrite usages of {@code com.google.common.collect.TreeTraverser} in terms of
 * {@code com.google.common.graph.Traverser}.
 */
@SuppressWarnings("DefaultPackage")
public class TraverserRewrite {
  abstract class TreeTraverserPreOrder<N> {
    @Placeholder
    abstract Iterable<N> getChildren(N node);

    @BeforeTemplate
    Iterable<N> before1(N root) {
      return TreeTraverser.using((N node) -> getChildren(node)).preOrderTraversal(root);
    }

    @BeforeTemplate
    Iterable<N> before2(N root) {
      return new TreeTraverser<N>() {
        @Override
        public Iterable<N> children(N node) {
          return getChildren(node);
        }
      }.preOrderTraversal(root);
    }

    @AfterTemplate
    Iterable<N> after(N root) {
      return Traverser.forTree((N node) -> getChildren(node)).depthFirstPreOrder(root);
    }
  }

  abstract class TreeTraverserPostOrder<N> {
    @Placeholder
    abstract Iterable<N> getChildren(N node);

    @BeforeTemplate
    Iterable<N> before1(N root) {
      return TreeTraverser.using((N node) -> getChildren(node)).postOrderTraversal(root);
    }

    @BeforeTemplate
    Iterable<N> before2(N root) {
      return new TreeTraverser<N>() {
        @Override
        public Iterable<N> children(N node) {
          return getChildren(node);
        }
      }.postOrderTraversal(root);
    }

    @AfterTemplate
    Iterable<N> after(N root) {
      return Traverser.forTree((N node) -> getChildren(node)).depthFirstPostOrder(root);
    }
  }

  abstract class TreeTraverserBreadthFirst<N> {
    @Placeholder
    abstract Iterable<N> getChildren(N node);

    @BeforeTemplate
    Iterable<N> before1(N root) {
      return TreeTraverser.using((N node) -> getChildren(node)).breadthFirstTraversal(root);
    }

    @BeforeTemplate
    Iterable<N> before2(N root) {
      return new TreeTraverser<N>() {
        @Override
        public Iterable<N> children(N node) {
          return getChildren(node);
        }
      }.breadthFirstTraversal(root);
    }

    @AfterTemplate
    Iterable<N> after(N root) {
      return Traverser.forTree((N node) -> getChildren(node)).breadthFirst(root);
    }
  }
}
