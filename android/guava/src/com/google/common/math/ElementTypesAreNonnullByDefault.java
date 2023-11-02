/*
 * Copyright (C) 2021 The Guava Authors
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

package com.google.common.math;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.common.annotations.GwtCompatible;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

/**
 * Marks all "top-level" types as non-null in a way that is recognized by Kotlin. Note that this
 * unfortunately includes type-variable usages, so we also provide {@link ParametricNullness} to
 * "undo" it as best we can.
 */
@GwtCompatible
@Retention(RUNTIME)
@Target(TYPE)
@TypeQualifierDefault({FIELD, METHOD, PARAMETER})
@Nonnull
@interface ElementTypesAreNonnullByDefault {}
