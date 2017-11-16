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

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import java.util.Map.Entry;

/**
 * This class implements the GWT serialization of {@link LinkedListMultimap}.
 *
 * @author Chris Povirk
 */
public class LinkedListMultimap_CustomFieldSerializer {

  public static void deserialize(SerializationStreamReader in, LinkedListMultimap<?, ?> out) {}

  public static LinkedListMultimap<Object, Object> instantiate(SerializationStreamReader in)
      throws SerializationException {
    LinkedListMultimap<Object, Object> multimap = LinkedListMultimap.create();
    int size = in.readInt();
    for (int i = 0; i < size; i++) {
      Object key = in.readObject();
      Object value = in.readObject();
      multimap.put(key, value);
    }
    return multimap;
  }

  public static void serialize(SerializationStreamWriter out, LinkedListMultimap<?, ?> multimap)
      throws SerializationException {
    out.writeInt(multimap.size());
    for (Entry<?, ?> entry : multimap.entries()) {
      out.writeObject(entry.getKey());
      out.writeObject(entry.getValue());
    }
  }
}
