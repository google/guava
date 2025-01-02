/*
 * Copyright (C) 2018 The Guava Authors
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
 * A dummy superclass of {@link ImmutableMultimap} that can be instanceof'd without ProGuard
 * retaining additional implementation details of {@link ImmutableMultimap}.
 */
@GwtCompatible
abstract class BaseImmutableMultimap<K, V> extends AbstractMultimap<K, V> {}
