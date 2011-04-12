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

package com.google.common.collect;

import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * This class implements the GWT serialization of
 * {@link EmptyImmutableMap}.
 * 
 * @author Chris Povirk
 */
public class EmptyImmutableMap_CustomFieldSerializer {

  public static void deserialize(SerializationStreamReader reader,
      EmptyImmutableMap instance) {
  }

  public static EmptyImmutableMap instantiate(
      SerializationStreamReader reader) {
    return EmptyImmutableMap.INSTANCE;
  }

  public static void serialize(SerializationStreamWriter writer,
      EmptyImmutableMap instance) {
  }
}
