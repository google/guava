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
import java.util.Comparator;

/**
 * This class implements the GWT serialization of {@link TreeBasedTable}.
 *
 * @author Hayward Chan
 */
public class TreeBasedTable_CustomFieldSerializer {
  public static void deserialize(SerializationStreamReader reader, TreeBasedTable<?, ?, ?> table) {}

  public static TreeBasedTable<Object, Object, Object> instantiate(SerializationStreamReader reader)
      throws SerializationException {
    @SuppressWarnings("unchecked") // The comparator isn't used statically.
    Comparator<Object> rowComparator = (Comparator<Object>) reader.readObject();
    @SuppressWarnings("unchecked") // The comparator isn't used statically.
    Comparator<Object> columnComparator = (Comparator<Object>) reader.readObject();

    TreeBasedTable<Object, Object, Object> table =
        TreeBasedTable.create(rowComparator, columnComparator);
    return Table_CustomFieldSerializerBase.populate(reader, table);
  }

  public static void serialize(SerializationStreamWriter writer, TreeBasedTable<?, ?, ?> table)
      throws SerializationException {
    writer.writeObject(table.rowComparator());
    writer.writeObject(table.columnComparator());
    Table_CustomFieldSerializerBase.serialize(writer, table);
  }
}
