/*
 * Copyright (C) 2012 The Guava Authors
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
import java.util.Map;

/**
 * This class implements the GWT serialization of {@link ImmutableEnumMap}.
 *
 * @author Louis Wasserman
 */
public class ImmutableEnumMap_CustomFieldSerializer {

  public static void deserialize(
      SerializationStreamReader reader, ImmutableEnumMap<?, ?> instance) {}

  public static <K extends Enum<K>, V> ImmutableEnumMap<?, ?> instantiate(
      SerializationStreamReader reader) throws SerializationException {
    Map<K, V> deserialized = Maps.newHashMap();
    Map_CustomFieldSerializerBase.deserialize(reader, deserialized);
    /*
     * It is safe to cast to ImmutableEnumSet because in order for it to be
     * serialized as an ImmutableEnumSet, it must be non-empty to start
     * with.
     */
    return (ImmutableEnumMap<?, ?>) Maps.immutableEnumMap(deserialized);
  }

  public static void serialize(SerializationStreamWriter writer, ImmutableEnumMap<?, ?> instance)
      throws SerializationException {
    Map_CustomFieldSerializerBase.serialize(writer, instance);
  }
}
