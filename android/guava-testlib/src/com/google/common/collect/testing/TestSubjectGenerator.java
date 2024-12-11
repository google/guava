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
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * To be implemented by test generators that can produce test subjects without requiring any
 * parameters.
 *
 * @param <T> the type created by this generator.
 * @author George van den Driessche
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public interface TestSubjectGenerator<T extends @Nullable Object> {
  T createTestSubject();
}
