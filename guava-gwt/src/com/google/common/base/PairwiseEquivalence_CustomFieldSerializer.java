/*
 * Copyright (C) 2011 The Guava Authors
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

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * GWT serialization logic for {@link PairwiseEquivalence}.
 *
 * @author Kedar Kanitkar
 */
public class PairwiseEquivalence_CustomFieldSerializer {

  private PairwiseEquivalence_CustomFieldSerializer() {}

  public static void deserialize(
      SerializationStreamReader reader, PairwiseEquivalence<?> instance) {}

  public static PairwiseEquivalence<?> instantiate(SerializationStreamReader reader)
      throws SerializationException {
    checkGwtRpcEnabled();
    return create((Equivalence<?>) reader.readObject());
  }

  private static <T> PairwiseEquivalence<T> create(Equivalence<T> elementEquivalence) {
    return new PairwiseEquivalence<T>(elementEquivalence);
  }

  public static void serialize(SerializationStreamWriter writer, PairwiseEquivalence<?> instance)
      throws SerializationException {
    checkGwtRpcEnabled();
    writer.writeObject(instance.elementEquivalence);
  }
}
