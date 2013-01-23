/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.core.java.util.Map_CustomFieldSerializerBase;

/**
 * This class contains static utility methods for writing {@link ImmutableTable} GWT field
 * serializers. Serializers should delegate to {@link #serialize} and {@link #instantiate}.
 *
 * @author Chris Povirk
 */
final class ImmutableTable_CustomFieldSerializerBase {
  public static ImmutableTable<Object, Object, Object> instantiate(SerializationStreamReader reader)
      throws SerializationException {
    ImmutableTable.Builder<Object, Object, Object> builder = ImmutableTable.builder();
    int rowCount = reader.readInt();
    for (int i = 0; i < rowCount; i++) {
      Object rowKey = reader.readObject();
      int colCount = reader.readInt();
      for (int j = 0; j < colCount; j++) {
        builder.put(rowKey, reader.readObject(), reader.readObject());
      }
    }
    return builder.build();
  }

  public static void serialize(
      SerializationStreamWriter writer, ImmutableTable<Object, Object, Object> table)
      throws SerializationException {
    writer.writeInt(table.rowKeySet().size());
    for (Object rowKey : table.rowKeySet()) {
      writer.writeObject(rowKey);
      Map_CustomFieldSerializerBase.serialize(writer, table.row(rowKey));
    }
  }

  private ImmutableTable_CustomFieldSerializerBase() {}
}
