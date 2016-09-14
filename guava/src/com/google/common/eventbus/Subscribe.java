/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.eventbus;

import com.google.common.annotations.Beta;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event subscriber.
 *
 * <p>The type of event will be indicated by the method's first (and only) parameter. If this
 * annotation is applied to methods with zero parameters, or more than one parameter, the object
 * containing the method will not be able to register for event delivery from the {@link EventBus}.
 *
 * <p>Unless also annotated with @{@link AllowConcurrentEvents}, event subscriber methods will be
 * invoked serially by each event bus that they are registered with.
 *
 * @author Cliff Biffle
 * @since 10.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Beta
public @interface Subscribe {}
