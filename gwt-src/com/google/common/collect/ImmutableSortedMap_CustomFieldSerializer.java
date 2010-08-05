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
import com.google.gwt.user.client.rpc.core.java.util.Map_CustomFieldSerializerBase;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This class implements the GWT serialization of {@link ImmutableSortedMap}.
 *
 * @author Chris Povirk
 */
public class ImmutableSortedMap_CustomFieldSerializer {
  public static void deserialize(SerializationStreamReader reader,
      ImmutableSortedMap<?, ?> instance) {
  }

  public static ImmutableSortedMap<?, ?> instantiate(
      SerializationStreamReader reader) throws SerializationException {
    /*
     * Nothing we can do, but we're already assuming the serialized form is
     * correctly typed, anyway.
     */
    @SuppressWarnings("unchecked")
    Comparator<Object> comparator = (Comparator<Object>) reader.readObject();

    SortedMap<Object, Object> entries = new TreeMap<Object, Object>(comparator);
    Map_CustomFieldSerializerBase.deserialize(reader, entries);

    return ImmutableSortedMap.orderedBy(comparator).putAll(entries).build();
  }

  public static void serialize(SerializationStreamWriter writer,
      ImmutableSortedMap<?, ?> instance) throws SerializationException {
    writer.writeObject(instance.comparator());

    Map_CustomFieldSerializerBase.serialize(writer, instance);
  }
}
