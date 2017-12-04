/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.testing.testers.CollectionAddAllTester.getAddAllUnsupportedNonePresentMethod;
import static com.google.common.collect.testing.testers.CollectionAddAllTester.getAddAllUnsupportedSomePresentMethod;
import static com.google.common.collect.testing.testers.CollectionAddTester.getAddUnsupportedNotPresentMethod;
import static com.google.common.collect.testing.testers.CollectionCreationTester.getCreateWithNullUnsupportedMethod;
import static com.google.common.collect.testing.testers.MapCreationTester.getCreateWithNullKeyUnsupportedMethod;
import static com.google.common.collect.testing.testers.MapEntrySetTester.getContainsEntryWithIncomparableKeyMethod;
import static com.google.common.collect.testing.testers.MapEntrySetTester.getContainsEntryWithIncomparableValueMethod;
import static com.google.common.collect.testing.testers.MapMergeTester.getMergeNullValueMethod;
import static com.google.common.collect.testing.testers.MapPutAllTester.getPutAllNullKeyUnsupportedMethod;
import static com.google.common.collect.testing.testers.MapPutTester.getPutNullKeyUnsupportedMethod;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import junit.framework.Test;

/**
 * Tests the {@link Map} implementations of {@link java.util}, suppressing tests that trip known
 * bugs in OpenJDK 6 or higher.
 *
 * @author Kevin Bourrillion
 */
/*
 * TODO(cpovirk): consider renaming this class in light of our now running it
 * under JDK7
 */
public class OpenJdk6MapTests extends TestsForMapsInJavaUtil {
  public static Test suite() {
    return new OpenJdk6MapTests().allTests();
  }

  @Override
  protected Collection<Method> suppressForTreeMapNatural() {
    return Arrays.asList(
        getPutNullKeyUnsupportedMethod(),
        getPutAllNullKeyUnsupportedMethod(),
        getCreateWithNullKeyUnsupportedMethod(),
        getCreateWithNullUnsupportedMethod(), // for keySet
        getContainsEntryWithIncomparableKeyMethod(),
        getContainsEntryWithIncomparableValueMethod());
  }

  @Override
  protected Collection<Method> suppressForConcurrentHashMap() {
    /*
     * The entrySet() of ConcurrentHashMap, unlike that of other Map
     * implementations, supports add() under JDK8. This seems problematic, but I
     * didn't see that discussed in the review, which included many other
     * changes: http://goo.gl/okTTdr
     *
     * TODO(cpovirk): decide what the best long-term action here is: force users
     * to suppress (as we do now), stop testing entrySet().add() at all, make
     * entrySet().add() tests tolerant of either behavior, introduce a map
     * feature for entrySet() that supports add(), or something else
     */
    return Arrays.asList(
        getAddUnsupportedNotPresentMethod(),
        getAddAllUnsupportedNonePresentMethod(),
        getAddAllUnsupportedSomePresentMethod());
  }

  @Override
  protected Collection<Method> suppressForConcurrentSkipListMap() {
    List<Method> methods = newArrayList();
    methods.addAll(super.suppressForConcurrentSkipListMap());
    methods.add(getContainsEntryWithIncomparableKeyMethod());
    methods.add(getContainsEntryWithIncomparableValueMethod());
    return methods;
  }

  @Override
  protected Collection<Method> suppressForHashtable() {
    return Arrays.asList(getMergeNullValueMethod());
  }
}
