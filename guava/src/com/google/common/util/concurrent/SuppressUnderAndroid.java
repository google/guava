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

package com.google.common.util.concurrent;

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
 * Signifies that a member should be stripped from the internal Android flavor of Guava. This
 * annotation is respected only by our Google-internal build system. Note that that system also
 * strips any test annotated with MediumTest or LargeTest.
 *
 * TODO(cpovirk): Replicate this revised description to other copies of the annotation, or better
 * yet, eliminate the need to use it in prod code, revert the description, and move it back to the
 * test directory.
 *
 * <p>For more discussion, see {@linkplain com.google.common.base.SuppressUnderAndroid the
 * documentation on another copy of this annotation}.
 */
@Retention(CLASS)
@Target({ANNOTATION_TYPE, CONSTRUCTOR, FIELD, METHOD, TYPE})
@GwtCompatible
@interface SuppressUnderAndroid {}
