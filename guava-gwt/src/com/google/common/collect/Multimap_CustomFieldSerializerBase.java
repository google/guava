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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import java.util.Collection;
import java.util.Map.Entry;

/**
 * This class contains static utility methods for writing {@code Multimap} GWT field serializers.
 * Serializers should delegate to {@link #serialize(SerializationStreamWriter, Multimap)} and to
 * either {@link #instantiate(SerializationStreamReader, ImmutableMultimap.Builder)} or {@link
 * #populate(SerializationStreamReader, Multimap)}.
 *
 * @author Chris Povirk
 */
public final class Multimap_CustomFieldSerializerBase {

  static ImmutableMultimap<Object, Object> instantiate(
      SerializationStreamReader reader, ImmutableMultimap.Builder<Object, Object> builder)
      throws SerializationException {
    int keyCount = reader.readInt();
    for (int i = 0; i < keyCount; ++i) {
      Object key = reader.readObject();
      int valueCount = reader.readInt();
      for (int j = 0; j < valueCount; ++j) {
        Object value = reader.readObject();
        builder.put(key, value);
      }
    }
    return builder.build();
  }

  @CanIgnoreReturnValue
  public static Multimap<Object, Object> populate(
      SerializationStreamReader reader, Multimap<Object, Object> multimap)
      throws SerializationException {
    int keyCount = reader.readInt();
    for (int i = 0; i < keyCount; ++i) {
      Object key = reader.readObject();
      int valueCount = reader.readInt();
      for (int j = 0; j < valueCount; ++j) {
        Object value = reader.readObject();
        multimap.put(key, value);
      }
    }
    return multimap;
  }

  public static void serialize(SerializationStreamWriter writer, Multimap<?, ?> instance)
      throws SerializationException {
    writer.writeInt(instance.asMap().size());
    for (Entry<?, ? extends Collection<?>> entry : instance.asMap().entrySet()) {
      writer.writeObject(entry.getKey());
      writer.writeInt(entry.getValue().size());
      for (Object value : entry.getValue()) {
        writer.writeObject(value);
      }
    }
  }

  private Multimap_CustomFieldSerializerBase() {}
}
