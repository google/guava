package com.google.common.collect;

import com.google.common.testing.CollectorTester;
import junit.framework.TestCase;

public final class AndroidAccessToCollectorsToImmutableListTest extends TestCase {

  public void testToImmutableListEmpty() {
    CollectorTester.of(AndroidAccessToCollectors.<String>toImmutableList())
        .expectCollects(ImmutableList.of());
  }

  public void testToImmutableListSingleton() {
    CollectorTester.of(AndroidAccessToCollectors.<String>toImmutableList())
        .expectCollects(ImmutableList.of("a"), "a");
  }

  public void testToImmutableList() {
    CollectorTester.of(AndroidAccessToCollectors.<String>toImmutableList())
        .expectCollects(ImmutableList.of("a", "b", "c", "d"), "a", "b", "c", "d");
  }
}
