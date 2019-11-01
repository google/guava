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

import static com.google.common.collect.Platform.checkGwtRpcEnabled;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.core.java.util.Collection_CustomFieldSerializerBase;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This class implements the GWT serialization of {@link RegularImmutableSortedSet}.
 *
 * @author Chris Povirk
 */
public class RegularImmutableSortedSet_CustomFieldSerializer {
  public static void deserialize(
      SerializationStreamReader reader, RegularImmutableSortedSet<?> instance) {}

  public static RegularImmutableSortedSet<Object> instantiate(SerializationStreamReader reader)
      throws SerializationException {
    checkGwtRpcEnabled();
    /*
     * Nothing we can do, but we're already assuming the serialized form is
     * correctly typed, anyway.
     */
    @SuppressWarnings("unchecked")
    Comparator<Object> comparator = (Comparator<Object>) reader.readObject();

    List<Object> elements = new ArrayList<>();
    Collection_CustomFieldSerializerBase.deserialize(reader, elements);
    /*
     * For this custom field serializer to be invoked, the set must have been
     * RegularImmutableSortedSet before it's serialized. Since
     * RegularImmutableSortedSet always have one or more elements,
     * ImmutableSortedSet.copyOf always return a RegularImmutableSortedSet back.
     */
    return (RegularImmutableSortedSet<Object>) ImmutableSortedSet.copyOf(comparator, elements);
  }

  public static void serialize(
      SerializationStreamWriter writer, RegularImmutableSortedSet<?> instance)
      throws SerializationException {
    checkGwtRpcEnabled();
    writer.writeObject(instance.comparator());

    Collection_CustomFieldSerializerBase.serialize(writer, instance);
  }
}
