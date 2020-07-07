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

/**
 * This class contains static utility methods for writing {@code Multiset} GWT field serializers.
 * Serializers should delegate to {@link #serialize(SerializationStreamWriter, Multiset)} and {@link
 * #populate(SerializationStreamReader, Multiset)}.
 *
 * @author Chris Povirk
 */
final class Multiset_CustomFieldSerializerBase {

  static Multiset<Object> populate(SerializationStreamReader reader, Multiset<Object> multiset)
      throws SerializationException {
    int distinctElements = reader.readInt();
    for (int i = 0; i < distinctElements; i++) {
      Object element = reader.readObject();
      int count = reader.readInt();
      multiset.add(element, count);
    }
    return multiset;
  }

  static void serialize(SerializationStreamWriter writer, Multiset<?> instance)
      throws SerializationException {
    int entryCount = instance.entrySet().size();
    writer.writeInt(entryCount);
    for (Multiset.Entry<?> entry : instance.entrySet()) {
      writer.writeObject(entry.getElement());
      writer.writeInt(entry.getCount());
    }
  }

  private Multiset_CustomFieldSerializerBase() {}
}
