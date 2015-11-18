/*
 * Copyright (C) 2015 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;

import java.util.Collections;
import java.util.Queue;

/**
 * An Iterator implementation which draws elements from a queue, removing them from the queue as it
 * iterates.
 */
@GwtCompatible
class ConsumingQueueIterator<T> extends AbstractIterator<T> {
  private final Queue<T> queue;

  ConsumingQueueIterator(T... elements) {
    this.queue = Platform.newFastestDeque(elements.length);
    Collections.addAll(queue, elements);
  }

  ConsumingQueueIterator(Queue<T> queue) {
    this.queue = checkNotNull(queue);
  }

  @Override
  public T computeNext() {
    return queue.isEmpty() ? endOfData() : queue.remove();
  }
}
