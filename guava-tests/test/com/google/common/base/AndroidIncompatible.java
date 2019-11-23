/*
 * Copyright (C) 2015 The Guava Authors
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

package com.google.common.base;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import com.google.common.annotations.GwtCompatible;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Signifies that a test should not be run under Android. This annotation is respected only by our
 * Google-internal Android suite generators. Note that those generators also suppress any test
 * annotated with MediumTest or LargeTest.
 *
 *
 * <p>Why use a custom annotation instead of {@code android.test.suitebuilder.annotation.Suppress}?
 * I'm not completely sure that this is the right choice, but it has various advantages:
 *
 * <ul>
 *   <li>An annotation named just "Suppress" might someday be treated by a non-Android tool as a
 *       suppression. This would follow the precedent of many of our annotation processors, which
 *       look for any annotation named, e.g., "GwtIncompatible," regardless of package.
 *   <li>An annotation named just "Suppress" might suggest to users that the test is suppressed
 *       under all environments. We could fight this by fully qualifying the annotation, but the
 *       result will be verbose and attention-grabbing.
 *   <li>We need to be careful about how we suppress {@code suite()} methods in {@code common.io}.
 *       The generated suite for {@code FooTest} ends up containing {@code FooTest} itself plus some
 *       other tests. We want to exclude the other tests (which Android can't handle) while
 *       continuing to run {@code FooTest} itself. This is exactly what happens with {@code
 *       AndroidIncompatible}. But I'm not sure what would happen if we annotated the {@code
 *       suite()} method with {@code Suppress}. Would {@code FooTest} itself be suppressed, too?
 *   <li>In at least one case, a use of {@code sun.misc.FpUtils}, the test will not even
 *       <i>compile</i> against Android. Now, this might be an artifact of our build system, one
 *       that we could probably work around. Or we could manually strip the test from open-source
 *       Guava while continuing to run it internally, as we do with many other tests. This would
 *       suffice because we our Android users and tests are using the open-source version, which
 *       would no longer have the problematic test. But why bother when we can instead strip it with
 *       a more precisely named annotation?
 *   <li>While a dependency on Android ought to be easy if it's for annotations only, it will
 *       probably require adding the dep to various ACLs, license files, and Proguard
 *       configurations, and there's always the potential that something will go wrong. It
 *       <i>probably</i> won't, since the deps are needed only in tests (and maybe someday in
 *       testlib), but why bother?
 *   <li>Stripping code entirely might help us keep under the method limit someday. Even if it never
 *       comes to that, it may at least help with build and startup times.
 * </ul>
 */
@Retention(CLASS)
@Target({ANNOTATION_TYPE, CONSTRUCTOR, FIELD, METHOD, TYPE})
@GwtCompatible
@interface AndroidIncompatible {}
