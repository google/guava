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

import java.util.Map;
import java.util.Map.Entry;

/**
 * This class implements the GWT serialization of {@link HashBasedTable}.
 *
 * @author Hayward Chan
 */
public class HashBasedTable_CustomFieldSerializer {

  public static void deserialize(SerializationStreamReader reader,
      HashBasedTable<?, ?, ?> instance) {
  }

  public static HashBasedTable<Object, Object, Object> instantiate(
      SerializationStreamReader reader) throws SerializationException {
    Map<?, ?> hashMap = (Map<?, ?>) reader.readObject();

    HashBasedTable<Object, Object, Object> table = HashBasedTable.create();
    for (Entry<?, ?> row : hashMap.entrySet()) {
      table.row(row.getKey()).putAll((Map<?, ?>) row.getValue());
    }
    return table;
  }

  public static void serialize(SerializationStreamWriter writer,
      HashBasedTable<?, ?, ?> instance)
      throws SerializationException {
    /*
     * The backing map of a HashBasedTable is a hash map of hash map.
     * Therefore, the backing map is serializable (assuming the row,
     * column and values are all serializable).
     */
    writer.writeObject(instance.backingMap);
  }
}
