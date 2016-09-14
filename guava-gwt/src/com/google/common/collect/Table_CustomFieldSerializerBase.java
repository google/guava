/*
 * Copyright (C) 2009 The Guava Authors
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
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class contains static utility methods for writing {@link Table} GWT field serializers.
 * Serializers should delegate to {@link #serialize} and {@link #populate}.
 *
 * <p>For {@link ImmutableTable}, see {@link ImmutableTable_CustomFieldSerializerBase}.
 *
 * @author Chris Povirk
 */
final class Table_CustomFieldSerializerBase {
  static <T extends StandardTable<Object, Object, Object>> T populate(
      SerializationStreamReader reader, T table) throws SerializationException {
    Map<?, ?> hashMap = (Map<?, ?>) reader.readObject();
    for (Entry<?, ?> row : hashMap.entrySet()) {
      table.row(row.getKey()).putAll((Map<?, ?>) row.getValue());
    }
    return table;
  }

  static void serialize(SerializationStreamWriter writer, StandardTable<?, ?, ?> table)
      throws SerializationException {
    /*
     * The backing map of a {Hash,Tree}BasedTable is a {Hash,Tree}Map of {Hash,Tree}Maps. Therefore,
     * the backing map is serializable (assuming that the row, column and values, along with any
     * comparators, are all serializable).
     */
    writer.writeObject(table.backingMap);
  }

  private Table_CustomFieldSerializerBase() {}
}
