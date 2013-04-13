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

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;

import junit.framework.TestCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests com.google.common.base.Suppliers.
 *
 * @author Laurence Gonsalves
 * @author Harry Heymann
 */
@GwtCompatible(emulated = true)
public class SuppliersTest extends TestCase {
  public void testCompose() {
    Supplier<Integer> fiveSupplier = new Supplier<Integer>() {
      @Override
      public Integer get() {
        return 5;
      }
    };

    Function<Number, Integer> intValueFunction =
        new Function<Number, Integer>() {
          @Override
          public Integer apply(Number x) {
            return x.intValue();
          }
        };

    Supplier<Integer> squareSupplier = Suppliers.compose(intValueFunction,
        fiveSupplier);

    assertEquals(Integer.valueOf(5), squareSupplier.get());
  }

  public void testComposeWithLists() {
    Supplier<ArrayList<Integer>> listSupplier
        = new Supplier<ArrayList<Integer>>() {
      @Override
      public ArrayList<Integer> get() {
        return Lists.newArrayList(0);
      }
    };

    Function<List<Integer>, List<Integer>> addElementFunction =
        new Function<List<Integer>, List<Integer>>() {
          @Override
          public List<Integer> apply(List<Integer> list) {
            ArrayList<Integer> result = Lists.newArrayList(list);
            result.add(1);
            return result;
          }
        };

    Supplier<List<Integer>> addSupplier = Suppliers.compose(addElementFunction,
        listSupplier);

    List<Integer> result = addSupplier.get();
    assertEquals(Integer.valueOf(0), result.get(0));
    assertEquals(Integer.valueOf(1), result.get(1));
  }

  static class CountingSupplier implements Supplier<Integer>, Serializable {
    private static final long serialVersionUID = 0L;
    transient int calls = 0;
    @Override
    public Integer get() {
      calls++;
      return calls * 10;
    }
  }

  public void testMemoize() {
    CountingSupplier countingSupplier = new CountingSupplier();
    Supplier<Integer> memoizedSupplier = Suppliers.memoize(countingSupplier);
    checkMemoize(countingSupplier, memoizedSupplier);
  }

  public void testMemoize_redudantly() {
    CountingSupplier countingSupplier = new CountingSupplier();
    Supplier<Integer> memoizedSupplier = Suppliers.memoize(countingSupplier);
    assertSame(memoizedSupplier, Suppliers.memoize(memoizedSupplier));
  }

  private void checkMemoize(
      CountingSupplier countingSupplier, Supplier<Integer> memoizedSupplier) {
    // the underlying supplier hasn't executed yet
    assertEquals(0, countingSupplier.calls);

    assertEquals(10, (int) memoizedSupplier.get());

    // now it has
    assertEquals(1, countingSupplier.calls);

    assertEquals(10, (int) memoizedSupplier.get());

    // it still should only have executed once due to memoization
    assertEquals(1, countingSupplier.calls);
  }

  public void testMemoizeExceptionThrown() {
    Supplier<Integer> exceptingSupplier = new Supplier<Integer>() {
      @Override
      public Integer get() {
        throw new NullPointerException();
      }
    };

    Supplier<Integer> memoizedSupplier = Suppliers.memoize(exceptingSupplier);

    // call get() twice to make sure that memoization doesn't interfere
    // with throwing the exception
    for (int i = 0; i < 2; i++) {
      try {
        memoizedSupplier.get();
        fail("failed to throw NullPointerException");
      } catch (NullPointerException e) {
        // this is what should happen
      }
    }
  }

  public void testOfInstanceSuppliesSameInstance() {
    Object toBeSupplied = new Object();
    Supplier<Object> objectSupplier = Suppliers.ofInstance(toBeSupplied);
    assertSame(toBeSupplied,objectSupplier.get());
    assertSame(toBeSupplied,objectSupplier.get()); // idempotent
  }

  public void testOfInstanceSuppliesNull() {
    Supplier<Integer> nullSupplier = Suppliers.ofInstance(null);
    assertNull(nullSupplier.get());
  }

  public void testSupplierFunction() {
    Supplier<Integer> supplier = Suppliers.ofInstance(14);
    Function<Supplier<Integer>, Integer> supplierFunction =
        Suppliers.supplierFunction();

    assertEquals(14, (int) supplierFunction.apply(supplier));
  }

  public void testOfInstance_equals() {
    new EqualsTester()
        .addEqualityGroup(
            Suppliers.ofInstance("foo"), Suppliers.ofInstance("foo"))
        .addEqualityGroup(Suppliers.ofInstance("bar"))
        .testEquals();
  }

  public void testCompose_equals() {
    new EqualsTester()
        .addEqualityGroup(
            Suppliers.compose(Functions.constant(1), Suppliers.ofInstance("foo")),
            Suppliers.compose(Functions.constant(1), Suppliers.ofInstance("foo")))
        .addEqualityGroup(
            Suppliers.compose(Functions.constant(2), Suppliers.ofInstance("foo")))
        .addEqualityGroup(
            Suppliers.compose(Functions.constant(1), Suppliers.ofInstance("bar")))
        .testEquals();
  }
}

