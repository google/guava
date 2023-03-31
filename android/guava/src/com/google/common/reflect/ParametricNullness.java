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

package com.google.common.reflect;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.common.annotations.GwtCompatible;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotates a "top-level" type-variable usage that takes its nullness from the type argument
 * supplied by the user of the class. For example, {@code Multiset.Entry.getElement()} returns
 * {@code @ParametricNullness E}, which means:
 *
 * <ul>
 *   <li>{@code getElement} on a {@code Multiset.Entry<@NonNull String>} returns {@code @NonNull
 *       String}.
 *   <li>{@code getElement} on a {@code Multiset.Entry<@Nullable String>} returns {@code @Nullable
 *       String}.
 * </ul>
 *
 * This is the same behavior as type-variable usages have to Kotlin and to the Checker Framework.
 * Contrast the method above to:
 *
 * <ul>
 *   <li>methods whose return type is a type variable but which can never return {@code null},
 *       typically because the type forbids nullable type arguments: For example, {@code
 *       ImmutableList.get} returns {@code E}, but that value is never {@code null}. (Accordingly,
 *       {@code ImmutableList} is declared to forbid {@code ImmutableList<@Nullable String>}.)
 *   <li>methods whose return type is a type variable but which can return {@code null} regardless
 *       of the type argument supplied by the user of the class: For example, {@code
 *       ImmutableMap.get} returns {@code @Nullable E} because the method can return {@code null}
 *       even on an {@code ImmutableMap<K, @NonNull String>}.
 * </ul>
 *
 * <p>Consumers of this annotation include:
 *
 * <ul>
 *   <li>Kotlin, for which it makes the type-variable usage (a) a Kotlin platform type when the type
 *       argument is non-nullable and (b) nullable when the type argument is nullable. We use this
 *       to "undo" {@link ElementTypesAreNonnullByDefault}. It is the best we can do for Kotlin
 *       under our current constraints.
 *   <li>NullAway, which will <a
 *       href="https://github.com/google/guava/issues/6126#issuecomment-1204399671">treat it
 *       identically to {@code Nullable} as of version 0.9.9</a>. To treat it that way before then,
 *       you can set {@code
 *       -XepOpt:NullAway:CustomNullableAnnotations=com.google.common.base.ParametricNullness,...,com.google.common.util.concurrent.ParametricNullness},
 *       where the {@code ...} contains the names of all the other {@code ParametricNullness}
 *       annotations in Guava. Or you might prefer to omit Guava from your {@code AnnotatedPackages}
 *       list.
 *   <li><a href="https://developers.google.com/j2objc">J2ObjC</a>
 *   <li>{@code NullPointerTester}, at least in the Android backport (where the type-use annotations
 *       {@code NullPointerTester} would need are not available) and in case of <a
 *       href="https://bugs.openjdk.java.net/browse/JDK-8202469">JDK-8202469</a>
 * </ul>
 *
 * <p>This annotation is a temporary hack. We will remove it after we're able to adopt the <a
 * href="https://jspecify.dev/">JSpecify</a> nullness annotations and <a
 * href="https://github.com/google/guava/issues/6126#issuecomment-1203145963">tools no longer need
 * it</a>.
 */
@GwtCompatible
@Retention(RUNTIME)
@Target({FIELD, METHOD, PARAMETER})
@javax.annotation.meta.TypeQualifierNickname
@javax.annotation.Nonnull(when = javax.annotation.meta.When.UNKNOWN)
@interface ParametricNullness {}
