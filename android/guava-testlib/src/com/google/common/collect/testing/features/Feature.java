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

package com.google.common.collect.testing.features;

import com.google.common.annotations.GwtCompatible;
import java.util.Set;

/**
 * Base class for enumerating the features of an interface to be tested.
 *
 * @param <T> The interface whose features are to be enumerated.
 * @author George van den Driessche
 */
@GwtCompatible
public interface Feature<T> {
  /** Returns the set of features that are implied by this feature. */
  Set<Feature<? super T>> getImpliedFeatures();
}
