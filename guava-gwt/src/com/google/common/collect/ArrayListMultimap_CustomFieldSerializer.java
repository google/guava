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

import static com.google.common.collect.Platform.checkGwtRpcEnabled;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * This class implements the GWT serialization of {@link ArrayListMultimap}.
 *
 * @author Chris Povirk
 */
public class ArrayListMultimap_CustomFieldSerializer {

  public static void deserialize(SerializationStreamReader in, ArrayListMultimap<?, ?> out) {}

  public static ArrayListMultimap<Object, Object> instantiate(SerializationStreamReader in)
      throws SerializationException {
    checkGwtRpcEnabled();
    return (ArrayListMultimap<Object, Object>)
        Multimap_CustomFieldSerializerBase.populate(in, ArrayListMultimap.create());
  }

  public static void serialize(SerializationStreamWriter out, ArrayListMultimap<?, ?> multimap)
      throws SerializationException {
    checkGwtRpcEnabled();
    Multimap_CustomFieldSerializerBase.serialize(out, multimap);
  }
}
