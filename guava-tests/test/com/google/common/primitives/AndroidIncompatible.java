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

package com.google.common.primitives;

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
 * <p>For more discussion, see {@linkplain com.google.common.base.AndroidIncompatible the
 * documentation on another copy of this annotation}.
 */
@Retention(CLASS)
@Target({ANNOTATION_TYPE, CONSTRUCTOR, FIELD, METHOD, TYPE})
@GwtCompatible
@interface AndroidIncompatible {}
