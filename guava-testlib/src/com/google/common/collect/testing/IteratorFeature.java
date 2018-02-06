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
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;

/**
 * A method supported by implementations of the {@link Iterator} or {@link ListIterator} interface.
 *
 * <p>This enum is GWT compatible.
 *
 * @author Chris Povirk
 */
@GwtCompatible
public enum IteratorFeature {
  /** Support for {@link Iterator#remove()}. */
  SUPPORTS_REMOVE,
  /**
   * Support for {@link ListIterator#add(Object)}; ignored for plain {@link Iterator}
   * implementations.
   */
  SUPPORTS_ADD,
  /**
   * Support for {@link ListIterator#set(Object)}; ignored for plain {@link Iterator}
   * implementations.
   */
  SUPPORTS_SET;

  /**
   * A set containing none of the optional features of the {@link Iterator} or {@link ListIterator}
   * interfaces.
   */
  public static final Set<IteratorFeature> UNMODIFIABLE = Collections.emptySet();

  /**
   * A set containing all of the optional features of the {@link Iterator} and {@link ListIterator}
   * interfaces.
   */
  public static final Set<IteratorFeature> MODIFIABLE =
      Collections.unmodifiableSet(EnumSet.allOf(IteratorFeature.class));
}
