/*
 * Copyright (C) 2012 The Guava Authors
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

import com.google.common.collect.CollectionBenchmarkSampleData.Element;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Helper classes for various benchmarks.
 * 
 * @author Christopher Swenson
 */
final class BenchmarkHelpers {
  /**
   * So far, this is the best way to test various implementations of {@link Set} subclasses.
   */
  public enum SetImpl {
    Hash {
      @Override Set<Element> create(Collection<Element> contents) {
        return new HashSet<Element>(contents);
      }
    },
    LinkedHash {
      @Override Set<Element> create(Collection<Element> contents) {
        return new LinkedHashSet<Element>(contents);
      }
    },
    Tree {
      @Override Set<Element> create(Collection<Element> contents) {
        return new TreeSet<Element>(contents);
      }
    },
    Unmodifiable {
      @Override Set<Element> create(Collection<Element> contents) {
        return Collections.unmodifiableSet(new HashSet<Element>(contents));
      }
    },
    Synchronized {
      @Override Set<Element> create(Collection<Element> contents) {
        return Collections.synchronizedSet(new HashSet<Element>(contents));
      }
    },
    Immutable {
      @Override Set<Element> create(Collection<Element> contents) {
        return ImmutableSet.copyOf(contents);
      }
    };

    abstract Set<Element> create(Collection<Element> contents);
  }
}
