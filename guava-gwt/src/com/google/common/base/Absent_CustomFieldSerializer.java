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

import static com.google.common.base.Platform.checkGwtRpcEnabled;

import com.google.common.annotations.GwtCompatible;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom GWT serializer for {@link Absent}.
 *
 * <p>GWT can serialize an absent {@code Optional} on its own, but the resulting object is a
 * different instance than the singleton {@code Absent.INSTANCE}, which breaks equality. We
 * implement a custom serializer to maintain the singleton property.
 *
 * @author Chris Povirk
 */
@GwtCompatible
public class Absent_CustomFieldSerializer {
  public static void deserialize(SerializationStreamReader reader, Absent<?> instance) {}

  public static Absent<?> instantiate(SerializationStreamReader reader) {
    checkGwtRpcEnabled();
    return Absent.INSTANCE;
  }

  public static void serialize(SerializationStreamWriter writer, Absent<?> instance) {
    checkGwtRpcEnabled();
  }
}
