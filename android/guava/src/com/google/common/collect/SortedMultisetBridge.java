/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.collect;

import com.google.common.annotations.GwtIncompatible;
import java.util.SortedSet;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Superinterface of {@link SortedMultiset} to introduce a bridge method for {@code elementSet()},
 * to ensure binary compatibility with older Guava versions that specified {@code elementSet()} to
 * return {@code SortedSet}.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
@ElementTypesAreNonnullByDefault
interface SortedMultisetBridge<E extends @Nullable Object> extends Multiset<E> {
  @Override
  SortedSet<E> elementSet();
}
