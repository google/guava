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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.util.Collection;

/**
 * Implementation of {@link ImmutableListMultimap} with no entries.
 *
 * @author Jared Levy
 */
@GwtCompatible(serializable = true)
@ElementTypesAreNonnullByDefault
class EmptyImmutableListMultimap extends ImmutableListMultimap<Object, Object> {
  static final EmptyImmutableListMultimap INSTANCE = new EmptyImmutableListMultimap();

  private EmptyImmutableListMultimap() {
    super(ImmutableMap.<Object, ImmutableList<Object>>of(), 0);
  }

  /*
   * TODO(b/242884182): Figure out why this helps produce the same class file when we compile most
   * of common.collect a second time with the results of the first compilation on the classpath. Or
   * just back this out once we stop doing that (which we'll do after our internal GWT setup
   * changes).
   */
  @Override
  public ImmutableMap<Object, Collection<Object>> asMap() {
    return super.asMap();
  }

  private Object readResolve() {
    return INSTANCE; // preserve singleton property
  }

  private static final long serialVersionUID = 0;
}
