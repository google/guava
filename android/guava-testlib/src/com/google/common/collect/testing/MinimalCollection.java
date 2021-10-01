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

package com.google.common.collect.testing;

import com.google.common.annotations.GwtCompatible;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * A simplistic collection which implements only the bare minimum allowed by the spec, and throws
 * exceptions whenever it can.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible
public class MinimalCollection<E> extends AbstractCollection<E> {
  // TODO: expose allow nulls parameter?

  public static <E> MinimalCollection<E> of(E... contents) {
    return new MinimalCollection<>(Object.class, true, contents);
  }

  // TODO: use this
  public static <E> MinimalCollection<E> ofClassAndContents(Class<? super E> type, E... contents) {
    return new MinimalCollection<>(type, true, contents);
  }

  private final E[] contents;
  private final Class<? super E> type;
  private final boolean allowNulls;

  // Package-private so that it can be extended.
  MinimalCollection(Class<? super E> type, boolean allowNulls, E... contents) {
    // TODO: consider making it shuffle the contents to test iteration order.
    this.contents = Platform.clone(contents);
    this.type = type;
    this.allowNulls = allowNulls;

    if (!allowNulls) {
      for (Object element : contents) {
        if (element == null) {
          throw new NullPointerException();
        }
      }
    }
  }

  @Override
  public int size() {
    return contents.length;
  }

  @Override
  public boolean contains(Object object) {
    if (!allowNulls) {
      // behave badly
      if (object == null) {
        throw new NullPointerException();
      }
    }
    Platform.checkCast(type, object); // behave badly
    return Arrays.asList(contents).contains(object);
  }

  @Override
  public boolean containsAll(Collection<?> collection) {
    if (!allowNulls) {
      for (Object object : collection) {
        // behave badly
        if (object == null) {
          throw new NullPointerException();
        }
      }
    }
    return super.containsAll(collection);
  }

  @Override
  public Iterator<E> iterator() {
    return Arrays.asList(contents).iterator();
  }

  @Override
  public Object[] toArray() {
    Object[] result = new Object[contents.length];
    System.arraycopy(contents, 0, result, 0, contents.length);
    return result;
  }

  /*
   * a "type A" unmodifiable collection freaks out proactively, even if there
   * wasn't going to be any actual work to do anyway
   */

  @Override
  public boolean addAll(Collection<? extends E> elementsToAdd) {
    throw up();
  }

  @Override
  public boolean removeAll(Collection<?> elementsToRemove) {
    throw up();
  }

  @Override
  public boolean retainAll(Collection<?> elementsToRetain) {
    throw up();
  }

  @Override
  public void clear() {
    throw up();
  }

  private static UnsupportedOperationException up() {
    throw new UnsupportedOperationException();
  }
}
