/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.collect.testing.testers;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractCollectionTester;
import com.google.common.collect.testing.Helpers;
import java.util.Collection;
import java.util.List;
import org.junit.Ignore;

/**
 * Base class for list testers.
 *
 * @author George van den Driessche
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class AbstractListTester<E> extends AbstractCollectionTester<E> {
  /*
   * Previously we had a field named list that was initialized to the value of
   * collection in setUp(), but that caused problems when a tester changed the
   * value of list or collection but not both.
   */
  protected final List<E> getList() {
    return (List<E>) collection;
  }

  /**
   * {@inheritDoc}
   *
   * <p>The {@code AbstractListTester} implementation overrides {@link
   * AbstractCollectionTester#expectContents(Collection)} to verify that the order of the elements
   * in the list under test matches what is expected.
   */
  @Override
  protected void expectContents(Collection<E> expectedCollection) {
    List<E> expectedList = Helpers.copyToList(expectedCollection);
    // Avoid expectEquals() here to delay reason manufacture until necessary.
    if (getList().size() != expectedList.size()) {
      fail("size mismatch: " + reportContext(expectedList));
    }
    for (int i = 0; i < expectedList.size(); i++) {
      E expected = expectedList.get(i);
      E actual = getList().get(i);
      if (expected != actual && (expected == null || !expected.equals(actual))) {
        fail("mismatch at index " + i + ": " + reportContext(expectedList));
      }
    }
  }

  /**
   * Used to delay string formatting until actually required, as it otherwise shows up in the test
   * execution profile when running an extremely large numbers of tests.
   */
  private String reportContext(List<E> expected) {
    return Platform.format(
        "expected collection %s; actual collection %s", expected, this.collection);
  }
}
