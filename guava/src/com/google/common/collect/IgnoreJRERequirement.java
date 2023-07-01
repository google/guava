/*
 * Copyright 2019 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Target;

/**
 * Disables Animal Sniffer's checking of compatibility with older versions of Java/Android.
 *
 * <p>Each package's copy of this annotation needs to be listed in our {@code pom.xml}.
 */
@Target({METHOD, CONSTRUCTOR, TYPE})
@ElementTypesAreNonnullByDefault
@interface IgnoreJRERequirement {}
