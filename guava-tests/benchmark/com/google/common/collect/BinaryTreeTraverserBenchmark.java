/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.common.base.Optional;
import com.google.common.primitives.Ints;
import java.util.List;
import java.util.Random;

/**
 * Benchmarks for the {@code TreeTraverser} operations on binary trees.
 *
 * @author Louis Wasserman
 */
public class BinaryTreeTraverserBenchmark {
  private static class BinaryNode {
    final int x;
    final Optional<BinaryNode> left;
    final Optional<BinaryNode> right;

    BinaryNode(int x, Optional<BinaryNode> left, Optional<BinaryNode> right) {
      this.x = x;
      this.left = left;
      this.right = right;
    }
  }

  enum Topology {
    BALANCED {
      @Override
      Optional<BinaryNode> createTree(int size, Random rng) {
        if (size == 0) {
          return Optional.absent();
        } else {
          int leftChildSize = (size - 1) / 2;
          int rightChildSize = size - 1 - leftChildSize;
          return Optional.of(
              new BinaryNode(
                  rng.nextInt(), createTree(leftChildSize, rng), createTree(rightChildSize, rng)));
        }
      }
    },
    ALL_LEFT {
      @Override
      Optional<BinaryNode> createTree(int size, Random rng) {
        Optional<BinaryNode> root = Optional.absent();
        for (int i = 0; i < size; i++) {
          root = Optional.of(new BinaryNode(rng.nextInt(), root, Optional.<BinaryNode>absent()));
        }
        return root;
      }
    },
    ALL_RIGHT {
      @Override
      Optional<BinaryNode> createTree(int size, Random rng) {
        Optional<BinaryNode> root = Optional.absent();
        for (int i = 0; i < size; i++) {
          root = Optional.of(new BinaryNode(rng.nextInt(), Optional.<BinaryNode>absent(), root));
        }
        return root;
      }
    },
    RANDOM {
      /**
       * Generates a tree with topology selected uniformly at random from the topologies of binary
       * trees of the specified size.
       */
      @Override
      Optional<BinaryNode> createTree(int size, Random rng) {
        int[] keys = new int[size];
        for (int i = 0; i < size; i++) {
          keys[i] = rng.nextInt();
        }
        return createTreap(Ints.asList(keys));
      }

      // See http://en.wikipedia.org/wiki/Treap for details on the algorithm.
      private Optional<BinaryNode> createTreap(List<Integer> keys) {
        if (keys.isEmpty()) {
          return Optional.absent();
        }
        int minIndex = 0;
        for (int i = 1; i < keys.size(); i++) {
          if (keys.get(i) < keys.get(minIndex)) {
            minIndex = i;
          }
        }
        Optional<BinaryNode> leftChild = createTreap(keys.subList(0, minIndex));
        Optional<BinaryNode> rightChild = createTreap(keys.subList(minIndex + 1, keys.size()));
        return Optional.of(new BinaryNode(keys.get(minIndex), leftChild, rightChild));
      }
    };

    abstract Optional<BinaryNode> createTree(int size, Random rng);
  }

  private static final TreeTraverser<BinaryNode> VIEWER =
      new TreeTraverser<BinaryNode>() {
        @Override
        public Iterable<BinaryNode> children(BinaryNode root) {
          return Optional.presentInstances(ImmutableList.of(root.left, root.right));
        }
      };

  enum Traversal {
    PRE_ORDER {
      @Override
      <T> Iterable<T> view(T root, TreeTraverser<T> viewer) {
        return viewer.preOrderTraversal(root);
      }
    },
    POST_ORDER {
      @Override
      <T> Iterable<T> view(T root, TreeTraverser<T> viewer) {
        return viewer.postOrderTraversal(root);
      }
    },
    BREADTH_FIRST {
      @Override
      <T> Iterable<T> view(T root, TreeTraverser<T> viewer) {
        return viewer.breadthFirstTraversal(root);
      }
    };

    abstract <T> Iterable<T> view(T root, TreeTraverser<T> viewer);
  }

  private Iterable<BinaryNode> view;

  @Param Topology topology;

  @Param({"1", "100", "10000", "1000000"})
  int size;

  @Param Traversal traversal;

  @Param({"1234"})
  SpecialRandom rng;

  @BeforeExperiment
  void setUp() {
    this.view = traversal.view(topology.createTree(size, rng).get(), VIEWER);
  }

  @Benchmark
  int traversal(int reps) {
    int tmp = 0;

    for (int i = 0; i < reps; i++) {
      for (BinaryNode node : view) {
        tmp += node.x;
      }
    }
    return tmp;
  }
}
