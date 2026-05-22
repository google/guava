/*
 * Copyright (C) 2026 The Guava Authors
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
import java.util.List;

/**
 * A {@link List} whose contents will never change. {@code ImmList} is the interface counterpart of
 * {@link ImmutableList}, allowing alternative implementations.
 *
 * <p>All mutation methods ({@link #add}, {@link #remove}, {@link #clear}, etc.) throw {@link
 * UnsupportedOperationException}.
 *
 * @param <E> the element type
 * @since 9999.0
 */
@GwtCompatible
public interface ImmList<E> extends List<E> {}
