/*
 * Copyright (C) 2010 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.core.java.util.Collection_CustomFieldSerializerBase;
import java.util.ArrayList;

/**
 * This class implements the GWT serialization of {@link RegularImmutableAsList}.
 *
 * @author Hayward Chan
 */
@GwtCompatible(emulated = true)
public class RegularImmutableAsList_CustomFieldSerializer {

  public static void deserialize(
      SerializationStreamReader reader, RegularImmutableAsList<?> instance) {}

  public static RegularImmutableAsList<Object> instantiate(SerializationStreamReader reader)
      throws SerializationException {
    checkGwtRpcEnabled();
    ArrayList<Object> elements = new ArrayList<>();
    Collection_CustomFieldSerializerBase.deserialize(reader, elements);
    ImmutableList<Object> delegate = ImmutableList.copyOf(elements);
    return new RegularImmutableAsList<>(delegate, delegate);
  }

  public static void serialize(SerializationStreamWriter writer, RegularImmutableAsList<?> instance)
      throws SerializationException {
    checkGwtRpcEnabled();
    Collection_CustomFieldSerializerBase.serialize(writer, instance);
  }
}
