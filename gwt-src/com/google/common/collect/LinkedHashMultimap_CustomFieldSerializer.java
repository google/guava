/*
 * Copyright (C) 2009 Google Inc.
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

import java.util.Map;

/**
 * This class implements the GWT serialization of {@link LinkedHashMultimap}.
 * 
 * @author Chris Povirk
 */
public class LinkedHashMultimap_CustomFieldSerializer {

  public static void deserialize(SerializationStreamReader in,
      LinkedHashMultimap<?, ?> out) {
  }

  public static LinkedHashMultimap<Object, Object> instantiate(
      SerializationStreamReader in) throws SerializationException {
    LinkedHashMultimap<Object, Object> multimap =
        (LinkedHashMultimap<Object, Object>)
            Multimap_CustomFieldSerializerBase.populate(
                in, LinkedHashMultimap.create());

    multimap.linkedEntries.clear(); // will clear and repopulate entries
    for (int i = 0; i < multimap.size(); i++) {
      Object key = in.readObject();
      Object value = in.readObject();
      multimap.linkedEntries.add(Maps.immutableEntry(key, value));
    }

    return multimap;
  }

  public static void serialize(SerializationStreamWriter out,
      LinkedHashMultimap<?, ?> multimap) throws SerializationException {
    Multimap_CustomFieldSerializerBase.serialize(out, multimap);
    for (Map.Entry<?, ?> entry : multimap.entries()) {
      out.writeObject(entry.getKey());
      out.writeObject(entry.getValue());
    }
  }
}
