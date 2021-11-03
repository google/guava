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

package com.google.common.graph;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.common.annotations.GwtCompatible;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a "top-level" type-variable usage as the closest we can get to "non-nullable when
 * non-nullable; nullable when nullable" (like the Android <a
 * href="https://android.googlesource.com/platform/libcore/+/master/luni/src/main/java/libcore/util/NullFromTypeParam.java">{@code
 * NullFromTypeParam}</a>).
 *
 * <p>Consumers of this annotation include:
 *
 * <ul>
 *   <li>Kotlin, for which it makes the type-variable usage (a) a Kotlin platform type when the type
 *       argument is non-nullable and (b) nullable when the type argument is nullable. We use this
 *       to "undo" {@link ElementTypesAreNonnullByDefault}.
 *   <li><a href="https://developers.google.com/j2objc">J2ObjC</a>
 *   <li>{@code NullPointerTester}, at least in the Android backport (where the type-use annotations
 *       {@code NullPointerTester} would need are not available) and in case of <a
 *       href="https://bugs.openjdk.java.net/browse/JDK-8202469">JDK-8202469</a>
 * </ul>
 *
 */
@GwtCompatible
@Retention(RUNTIME)
@Target({FIELD, METHOD, PARAMETER})
@javax.annotation.meta.TypeQualifierNickname
@javax.annotation.Nonnull(when = javax.annotation.meta.When.UNKNOWN)
@interface ParametricNullness {}
