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

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class implements the GWT serialization of {@link TreeBasedTable}.
 *
 * @author Hayward Chan
 */
public class TreeBasedTable_CustomFieldSerializer {

  public static void deserialize(SerializationStreamReader reader,
      TreeBasedTable<?, ?, ?> instance) {
  }

  public static TreeBasedTable<Object, Object, Object> instantiate(
      SerializationStreamReader reader) throws SerializationException {
    @SuppressWarnings("unchecked") // The comparator isn't used statically.
    Comparator<Object> rowComparator
        = (Comparator<Object>) reader.readObject();
    @SuppressWarnings("unchecked") // The comparator isn't used statically.
    Comparator<Object> columnComparator
        = (Comparator<Object>) reader.readObject();
    Map<?, ?> backingMap = (Map<?, ?>) reader.readObject();

    TreeBasedTable<Object, Object, Object> table
        = TreeBasedTable.create(rowComparator, columnComparator);
    for (Entry<?, ?> row : backingMap.entrySet()) {
      table.row(row.getKey()).putAll((Map<?, ?>) row.getValue());
    }
    return table;
  }

  public static void serialize(SerializationStreamWriter writer,
      TreeBasedTable<?, ?, ?> instance)
      throws SerializationException {
    /*
     * The backing map of a TreeBasedTable is a tree map of tree map.
     * Therefore, the backing map is GWT serializable (assuming the row,
     * column, value, the row comparator and column comparator are all
     * serializable).
     */
    writer.writeObject(instance.rowComparator());
    writer.writeObject(instance.columnComparator());
    writer.writeObject(instance.backingMap);
  }
}
