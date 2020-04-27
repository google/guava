package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import junit.framework.TestCase;

import java.util.List;

@GwtCompatible(emulated = true)
public class CartesianListTest extends TestCase {

  public void testIndexOf() {
    ImmutableList<String> strings1 = ImmutableList.of("a", "b", "c");
    ImmutableList<String> strings2 = ImmutableList.of("c", "c");
    ImmutableList<String> strings3 = ImmutableList.of("b", "b", "c");
    List<List<String>> product = Lists.cartesianProduct(strings1, strings2, strings3);
    assertEquals(-1, product.indexOf(ImmutableList.of("a")));
    assertEquals(-1, product.indexOf(ImmutableList.of("a", "b", "c", "a")));
    assertEquals(-1, product.indexOf(ImmutableSet.of("a", "c", "c")));
    assertEquals(2, product.indexOf(ImmutableList.of("a", "c", "c")));
    assertEquals(6, product.indexOf(ImmutableList.of("b", "c", "b")));
    assertEquals(14, product.indexOf(ImmutableList.of("c", "c", "c")));
    assertEquals(0, product.indexOf(ImmutableList.of("a", "c", "b")));
  }

  public void testIndexOf_emptyProduct() {
    ImmutableList<String> strings1 = ImmutableList.of("a", "b", "c");
    ImmutableList<String> strings2 = ImmutableList.of();
    List<List<String>> product = Lists.cartesianProduct(strings1, strings2);
    assertEquals(-1, product.indexOf(ImmutableList.of()));
  }

  public void testLastIndexOf() {
    ImmutableList<String> strings1 = ImmutableList.of("a", "b", "c");
    ImmutableList<String> strings2 = ImmutableList.of("c", "c");
    ImmutableList<String> strings3 = ImmutableList.of("b", "b", "c");
    List<List<String>> product = Lists.cartesianProduct(strings1, strings2, strings3);
    assertEquals(-1, product.lastIndexOf(ImmutableList.of("a")));
    assertEquals(-1, product.lastIndexOf(ImmutableList.of("a", "b", "c", "a")));
    assertEquals(-1, product.lastIndexOf(ImmutableSet.of("a", "c", "c")));
    assertEquals(5, product.lastIndexOf(ImmutableList.of("a", "c", "c")));
    assertEquals(10, product.lastIndexOf(ImmutableList.of("b", "c", "b")));
    assertEquals(17, product.lastIndexOf(ImmutableList.of("c", "c", "c")));
    assertEquals(4, product.lastIndexOf(ImmutableList.of("a", "c", "b")));
  }

  public void testLastIndexOf_emptyProduct() {
    ImmutableList<String> strings1 = ImmutableList.of("a", "b", "c");
    ImmutableList<String> strings2 = ImmutableList.of();
    List<List<String>> product = Lists.cartesianProduct(strings1, strings2);
    assertEquals(-1, product.lastIndexOf(ImmutableList.of()));
  }
}
