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

package com.google.common.collect.testing;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Ignore;

/**
 * Base class for testers of classes (including {@link Collection} and {@link java.util.Map Map})
 * that contain elements.
 *
 * @param <C> the type of the container
 * @param <E> the type of the container's contents
 * @author George van den Driessche
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public abstract class AbstractContainerTester<C, E>
    extends AbstractTester<OneSizeTestContainerGenerator<C, E>> {
  protected SampleElements<E> samples;
  protected C container;

  @Override
  @OverridingMethodsMustInvokeSuper
  public void setUp() throws Exception {
    super.setUp();
    samples = this.getSubjectGenerator().samples();
    resetContainer();
  }

  /**
   * @return the contents of the container under test, for use by {@link #expectContents(Object[])
   *     expectContents(E...)} and its friends.
   */
  protected abstract Collection<E> actualContents();

  /**
   * Replaces the existing container under test with a new container created by the subject
   * generator.
   *
   * @see #resetContainer(Object) resetContainer(C)
   * @return the new container instance.
   */
  @CanIgnoreReturnValue
  protected C resetContainer() {
    return resetContainer(getSubjectGenerator().createTestSubject());
  }

  /**
   * Replaces the existing container under test with a new container. This is useful when a single
   * test method needs to create multiple containers while retaining the ability to use {@link
   * #expectContents(Object[]) expectContents(E...)} and other convenience methods. The creation of
   * multiple containers in a single method is discouraged in most cases, but it is vital to the
   * iterator tests.
   *
   * @return the new container instance
   * @param newValue the new container instance
   */
  @CanIgnoreReturnValue
  protected C resetContainer(C newValue) {
    container = newValue;
    return container;
  }

  /**
   * @see #expectContents(java.util.Collection)
   * @param elements expected contents of {@link #container}
   */
  protected final void expectContents(E... elements) {
    expectContents(Arrays.asList(elements));
  }

  /**
   * Asserts that the collection under test contains exactly the given elements, respecting
   * cardinality but not order. Subclasses may override this method to provide stronger assertions,
   * e.g., to check ordering in lists, but realize that <strong>unless a test extends {@link
   * com.google.common.collect.testing.testers.AbstractListTester AbstractListTester}, a call to
   * {@code expectContents()} invokes this version</strong>.
   *
   * @param expected expected value of {@link #container}
   */
  /*
   * TODO: improve this and other implementations and move out of this framework
   * for wider use
   *
   * TODO: could we incorporate the overriding logic from AbstractListTester, by
   * examining whether the features include KNOWN_ORDER?
   */
  protected void expectContents(Collection<E> expected) {
    Helpers.assertEqualIgnoringOrder(expected, actualContents());
  }

  protected void expectUnchanged() {
    expectContents(getOrderedElements());
  }

  /**
   * Asserts that the collection under test contains exactly the elements it was initialized with
   * plus the given elements, according to {@link #expectContents(java.util.Collection)}. In other
   * words, for the default {@code expectContents()} implementation, the number of occurrences of
   * each given element has increased by one since the test collection was created, and the number
   * of occurrences of all other elements has not changed.
   *
   * <p>Note: This means that a test like the following will fail if {@code collection} is a {@code
   * Set}:
   *
   * <pre>
   * collection.add(existingElement);
   * expectAdded(existingElement);</pre>
   *
   * <p>In this case, {@code collection} was not modified as a result of the {@code add()} call, and
   * the test will fail because the number of occurrences of {@code existingElement} is unchanged.
   *
   * @param elements expected additional contents of {@link #container}
   */
  protected final void expectAdded(E... elements) {
    List<E> expected = Helpers.copyToList(getSampleElements());
    expected.addAll(Arrays.asList(elements));
    expectContents(expected);
  }

  protected final void expectAdded(int index, E... elements) {
    expectAdded(index, Arrays.asList(elements));
  }

  protected final void expectAdded(int index, Collection<E> elements) {
    List<E> expected = Helpers.copyToList(getSampleElements());
    expected.addAll(index, elements);
    expectContents(expected);
  }

  /*
   * TODO: if we're testing a list, we could check indexOf(). (Doing it in
   * AbstractListTester isn't enough because many tests that run on lists don't
   * extends AbstractListTester.) We could also iterate over all elements to
   * verify absence
   */
  protected void expectMissing(E... elements) {
    for (E element : elements) {
      assertFalse("Should not contain " + element, actualContents().contains(element));
    }
  }

  protected E[] createSamplesArray() {
    E[] array = getSubjectGenerator().createArray(getNumElements());
    getSampleElements().toArray(array);
    return array;
  }

  protected E[] createOrderedArray() {
    E[] array = getSubjectGenerator().createArray(getNumElements());
    getOrderedElements().toArray(array);
    return array;
  }

  public static class ArrayWithDuplicate<E> {
    public final E[] elements;
    public final E duplicate;

    private ArrayWithDuplicate(E[] elements, E duplicate) {
      this.elements = elements;
      this.duplicate = duplicate;
    }
  }

  /**
   * @return an array of the proper size with a duplicate element. The size must be at least three.
   */
  protected ArrayWithDuplicate<E> createArrayWithDuplicateElement() {
    E[] elements = createSamplesArray();
    E duplicate = elements[(elements.length / 2) - 1];
    elements[(elements.length / 2) + 1] = duplicate;
    return new ArrayWithDuplicate<>(elements, duplicate);
  }

  // Helper methods to improve readability of derived classes

  protected int getNumElements() {
    return getSubjectGenerator().getCollectionSize().getNumElements();
  }

  protected Collection<E> getSampleElements(int howMany) {
    return getSubjectGenerator().getSampleElements(howMany);
  }

  protected Collection<E> getSampleElements() {
    return getSampleElements(getNumElements());
  }

  /**
   * Returns the {@linkplain #getSampleElements() sample elements} as ordered by {@link
   * TestContainerGenerator#order(List)}. Tests should use this method only if they declare
   * requirement {@link com.google.common.collect.testing.features.CollectionFeature#KNOWN_ORDER}.
   */
  protected List<E> getOrderedElements() {
    List<E> list = new ArrayList<>();
    for (E e : getSubjectGenerator().order(new ArrayList<E>(getSampleElements()))) {
      list.add(e);
    }
    return Collections.unmodifiableList(list);
  }

  /**
   * @return a suitable location for a null element, to use when initializing containers for tests
   *     that involve a null element being present.
   */
  protected int getNullLocation() {
    return getNumElements() / 2;
  }

  @SuppressWarnings("unchecked")
  protected MinimalCollection<E> createDisjointCollection() {
    return MinimalCollection.of(e3(), e4());
  }

  @SuppressWarnings("unchecked")
  protected MinimalCollection<E> emptyCollection() {
    return MinimalCollection.<E>of();
  }

  protected final E e0() {
    return samples.e0();
  }

  protected final E e1() {
    return samples.e1();
  }

  protected final E e2() {
    return samples.e2();
  }

  protected final E e3() {
    return samples.e3();
  }

  protected final E e4() {
    return samples.e4();
  }
}
