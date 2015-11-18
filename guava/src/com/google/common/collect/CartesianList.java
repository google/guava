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

import static com.google.common.base.Preconditions.checkElementIndex;

import com.google.common.annotations.GwtCompatible;
import com.google.common.math.IntMath;

import java.util.AbstractList;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

import javax.annotation.Nullable;

/**
 * Implementation of {@link Lists#cartesianProduct(List)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
final class CartesianList<E> extends AbstractList<List<E>> implements RandomAccess {

  private transient final ImmutableList<List<E>> axes;
  private transient final int[] axesSizeProduct;

  static <E> List<List<E>> create(List<? extends List<? extends E>> lists) {
    ImmutableList.Builder<List<E>> axesBuilder = new ImmutableList.Builder<List<E>>(lists.size());
    for (List<? extends E> list : lists) {
      List<E> copy = ImmutableList.copyOf(list);
      if (copy.isEmpty()) {
        return ImmutableList.of();
      }
      axesBuilder.add(copy);
    }
    return new CartesianList<E>(axesBuilder.build());
  }

  CartesianList(ImmutableList<List<E>> axes) {
    this.axes = axes;
    int[] axesSizeProduct = new int[axes.size() + 1];
    axesSizeProduct[axes.size()] = 1;
    try {
      for (int i = axes.size() - 1; i >= 0; i--) {
        axesSizeProduct[i] = IntMath.checkedMultiply(axesSizeProduct[i + 1], axes.get(i).size());
      }
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException(
          "Cartesian product too large; must have size at most Integer.MAX_VALUE");
    }
    this.axesSizeProduct = axesSizeProduct;
  }

  private int getAxisIndexForProductIndex(int index, int axis) {
    return (index / axesSizeProduct[axis + 1]) % axes.get(axis).size();
  }

  @Override
  public ImmutableList<E> get(final int index) {
    checkElementIndex(index, size());
    return new ImmutableList<E>() {

      @Override
      public int size() {
        return axes.size();
      }

      @Override
      public E get(int axis) {
        checkElementIndex(axis, size());
        int axisIndex = getAxisIndexForProductIndex(index, axis);
        return axes.get(axis).get(axisIndex);
      }

      @Override
      boolean isPartialView() {
        return true;
      }
    };
  }

  @Override
  public int size() {
    return axesSizeProduct[0];
  }

  @Override
  public boolean contains(@Nullable Object o) {
    if (!(o instanceof List)) {
      return false;
    }
    List<?> list = (List<?>) o;
    if (list.size() != axes.size()) {
      return false;
    }
    ListIterator<?> itr = list.listIterator();
    while (itr.hasNext()) {
      int index = itr.nextIndex();
      if (!axes.get(index).contains(itr.next())) {
        return false;
      }
    }
    return true;
  }
}
