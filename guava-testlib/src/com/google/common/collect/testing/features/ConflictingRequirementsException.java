/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.collect.testing.features;

import com.google.common.annotations.GwtCompatible;
import java.util.Set;

/**
 * Thrown when requirements on a tester method or class conflict with each other.
 *
 * @author George van den Driessche
 */
@GwtCompatible
public class ConflictingRequirementsException extends Exception {
  private Set<Feature<?>> conflicts;
  private Object source;

  public ConflictingRequirementsException(
      String message, Set<Feature<?>> conflicts, Object source) {
    super(message);
    this.conflicts = conflicts;
    this.source = source;
  }

  public Set<Feature<?>> getConflicts() {
    return conflicts;
  }

  public Object getSource() {
    return source;
  }

  @Override
  public String getMessage() {
    return super.getMessage() + " (source: " + source + ")";
  }

  private static final long serialVersionUID = 0;
}
