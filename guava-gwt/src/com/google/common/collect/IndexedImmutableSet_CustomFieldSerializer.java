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

/**
 * Dummy serializer. Otherwise, GWT, in processing JdkBackedImmutableSet -- even though that class
 * has a custom field serializer -- would generate its own version of this class, implemented in
 * terms of calls to ImmutableSet_CustomFieldSerializer, which is itself a dummy that we've
 * provided. That produces GWT compilation errors, albeit ones that are non-fatal (even with -strict
 * on, oddly).
 */
public final class IndexedImmutableSet_CustomFieldSerializer {}
