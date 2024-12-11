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

package com.google.common.collect.testing.google;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multiset;
import com.google.common.collect.testing.TestCollectionGenerator;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Creates multisets, containing sample elements, to be tested.
 *
 * @author Jared Levy
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public interface TestMultisetGenerator<E extends @Nullable Object>
    extends TestCollectionGenerator<E> {
  @Override
  Multiset<E> create(Object... elements);
}
