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

import static com.google.common.collect.testing.testers.CollectionToArrayTester.getToArrayIsPlainObjectArrayMethod;
import static com.google.common.collect.testing.testers.ListAddTester.getAddSupportedNullPresentMethod;
import static com.google.common.collect.testing.testers.ListSetTester.getSetNullSupportedMethod;

import com.google.common.collect.testing.testers.CollectionAddTester;
import com.google.common.collect.testing.testers.ListAddAtIndexTester;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import junit.framework.Test;

/**
 * Tests the {@link List} implementations of {@link java.util}, suppressing tests that trip known
 * OpenJDK 6 bugs.
 *
 * @author Kevin Bourrillion
 */
public class OpenJdk6ListTests extends TestsForListsInJavaUtil {
  public static Test suite() {
    return new OpenJdk6ListTests().allTests();
  }

  @Override
  protected Collection<Method> suppressForArraysAsList() {
    return Arrays.asList(getToArrayIsPlainObjectArrayMethod());
  }

  @Override
  protected Collection<Method> suppressForCheckedList() {
    return Arrays.asList(
        CollectionAddTester.getAddNullSupportedMethod(),
        getAddSupportedNullPresentMethod(),
        ListAddAtIndexTester.getAddNullSupportedMethod(),
        getSetNullSupportedMethod());
  }
}
