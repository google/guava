/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.testing;

import com.google.common.annotations.GwtCompatible;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * An {@link ClusterException} is a data structure that allows for some code to "throw multiple
 * exceptions", or something close to it. The prototypical code that calls for this class is
 * presented below:
 *
 * <pre>
 * void runManyThings({@literal List<ThingToRun>} thingsToRun) {
 *   for (ThingToRun thingToRun : thingsToRun) {
 *     thingToRun.run(); // say this may throw an exception, but you want to
 *                       // always run all thingsToRun
 *   }
 * }
 * </pre>
 *
 * <p>This is what the code would become:
 *
 * <pre>
 * void runManyThings({@literal List<ThingToRun>} thingsToRun) {
 *   {@literal List<Exception>} exceptions = Lists.newArrayList();
 *   for (ThingToRun thingToRun : thingsToRun) {
 *     try {
 *       thingToRun.run();
 *     } catch (Exception e) {
 *       exceptions.add(e);
 *     }
 *   }
 *   if (exceptions.size() &gt; 0) {
 *     throw ClusterException.create(exceptions);
 *   }
 * }
 * </pre>
 *
 * <p>See semantic details at {@link #create(Collection)}.
 *
 * @author Luiz-Otavio Zorzella
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
final class ClusterException extends RuntimeException {

  final Collection<? extends Throwable> exceptions;

  private ClusterException(Collection<? extends Throwable> exceptions) {
    super(
        exceptions.size() + " exceptions were thrown. The first exception is listed as a cause.",
        exceptions.iterator().next());
    ArrayList<? extends Throwable> temp = new ArrayList<>(exceptions);
    this.exceptions = Collections.unmodifiableCollection(temp);
  }

  /** See {@link #create(Collection)}. */
  static RuntimeException create(Throwable... exceptions) {
    ArrayList<Throwable> temp = new ArrayList<>(Arrays.asList(exceptions));
    return create(temp);
  }

  /**
   * Given a collection of exceptions, returns a {@link RuntimeException}, with the following rules:
   *
   * <ul>
   *   <li>If {@code exceptions} has a single exception and that exception is a {@link
   *       RuntimeException}, return it
   *   <li>If {@code exceptions} has a single exceptions and that exceptions is <em>not</em> a
   *       {@link RuntimeException}, return a simple {@code RuntimeException} that wraps it
   *   <li>Otherwise, return an instance of {@link ClusterException} that wraps the first exception
   *       in the {@code exceptions} collection.
   * </ul>
   *
   * <p>Though this method takes any {@link Collection}, it often makes most sense to pass a {@link
   * java.util.List} or some other collection that preserves the order in which the exceptions got
   * added.
   *
   * @throws NullPointerException if {@code exceptions} is null
   * @throws IllegalArgumentException if {@code exceptions} is empty
   */
  static RuntimeException create(Collection<? extends Throwable> exceptions) {
    if (exceptions.size() == 0) {
      throw new IllegalArgumentException("Can't create an ExceptionCollection with no exceptions");
    }
    if (exceptions.size() == 1) {
      Throwable temp = exceptions.iterator().next();
      if (temp instanceof RuntimeException) {
        return (RuntimeException) temp;
      } else {
        return new RuntimeException(temp);
      }
    }
    return new ClusterException(exceptions);
  }
}
