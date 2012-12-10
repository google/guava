/*
 * Copyright (C) 2010 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;

/**
 * Factories for common {@link DiscreteDomain} instances.
 *
 * <p>See the Guava User Guide section on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/RangesExplained#Discrete_Domains">
 * {@code DiscreteDomain}</a>.
 *
 * @author Gregory Kick
 * @since 10.0
 * @deprecated Merged into {@link DiscreteDomain}.  This class is scheduled for deletion in release
 *             15.0.
 */
@GwtCompatible
@Deprecated
public final class DiscreteDomains {
  private DiscreteDomains() {}

  /**
   * Returns the discrete domain for values of type {@code Integer}.
   */
  public static DiscreteDomain<Integer> integers() {
    return DiscreteDomain.integers();
  }

  /**
   * Returns the discrete domain for values of type {@code Long}.
   */
  public static DiscreteDomain<Long> longs() {
    return DiscreteDomain.longs();
  }
}
