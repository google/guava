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

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom GWT serializer for {@link Present}.
 *
 * @author Chris Povirk
 */
@GwtCompatible
public class Present_CustomFieldSerializer {
  public static void deserialize(SerializationStreamReader reader, Present<?> instance) {}

  public static Present<Object> instantiate(SerializationStreamReader reader)
      throws SerializationException {
    return (Present<Object>) Optional.of(reader.readObject());
  }

  public static void serialize(SerializationStreamWriter writer, Present<?> instance)
      throws SerializationException {
    writer.writeObject(instance.get());
  }
}
