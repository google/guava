/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;

/**
 * Contains dummy collection implementations to convince GWT that part of
 * serializing a collection is serializing its elements.
 *
 * <p>See {@linkplain com.google.common.collect.GwtSerializationDependencies the
 * com.google.common.collect version} for more details.
 *
 * @author Chris Povirk
 */
@GwtCompatible
// None of these classes are instantiated, let alone serialized:
@SuppressWarnings("serial")
final class GwtSerializationDependencies {
  private GwtSerializationDependencies() {}
}
