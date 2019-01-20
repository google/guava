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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class implements the GWT serialization of {@link LinkedHashMultimap}.
 *
 * @author Chris Povirk
 */
public class LinkedHashMultimap_CustomFieldSerializer {

  public static void deserialize(SerializationStreamReader in, LinkedHashMultimap<?, ?> out) {}

  public static LinkedHashMultimap<Object, Object> instantiate(SerializationStreamReader stream)
      throws SerializationException {
    LinkedHashMultimap<Object, Object> multimap = LinkedHashMultimap.create();

    int distinctKeys = stream.readInt();
    Map<Object, Collection<Object>> map = new LinkedHashMap<>();
    for (int i = 0; i < distinctKeys; i++) {
      Object key = stream.readObject();
      map.put(key, multimap.createCollection(key));
    }
    int entries = stream.readInt();
    for (int i = 0; i < entries; i++) {
      Object key = stream.readObject();
      Object value = stream.readObject();
      map.get(key).add(value);
    }
    multimap.setMap(map);

    return multimap;
  }

  public static void serialize(SerializationStreamWriter stream, LinkedHashMultimap<?, ?> multimap)
      throws SerializationException {
    stream.writeInt(multimap.keySet().size());
    for (Object key : multimap.keySet()) {
      stream.writeObject(key);
    }
    stream.writeInt(multimap.size());
    for (Entry<?, ?> entry : multimap.entries()) {
      stream.writeObject(entry.getKey());
      stream.writeObject(entry.getValue());
    }
  }
}
