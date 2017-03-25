/*
 * Copyright (C) 2016 The Guava Authors
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

/**
 * This class implements the GWT serialization of {@link Range}.
 *
 * @author Dean de Bree
 */
public class Range_CustomFieldSerializer {

  public static void deserialize(SerializationStreamReader reader, Range<?> instance) {}

  public static Range<?> instantiate(SerializationStreamReader reader)
      throws SerializationException {

    Cut lowerBound;
    boolean hasLowerBound = reader.readBoolean();
    if (hasLowerBound) {
      boolean lowerIsClosed = reader.readBoolean();
      Comparable lower = (Comparable) reader.readObject();

      lowerBound = lowerIsClosed ? Cut.belowValue(lower) : Cut.aboveValue(lower);
    } else {
      lowerBound = Cut.belowAll();
    }

    Cut upperBound;
    boolean hasUpperBound = reader.readBoolean();
    if (hasUpperBound) {
      boolean upperIsClosed = reader.readBoolean();
      Comparable upper = (Comparable) reader.readObject();

      upperBound = upperIsClosed ? Cut.aboveValue(upper) : Cut.belowValue(upper);
    } else {
      upperBound = Cut.aboveAll();
    }

    return Range.create(lowerBound, upperBound);
  }

  public static void serialize(SerializationStreamWriter writer, Range<?> instance)
      throws SerializationException {

    if (instance.hasLowerBound()) {
      writer.writeBoolean(true);
      writer.writeBoolean(instance.lowerBoundType() == BoundType.CLOSED);
      writer.writeObject(instance.lowerEndpoint());
    } else {
      writer.writeBoolean(false);
    }

    if (instance.hasUpperBound()) {
      writer.writeBoolean(true);
      writer.writeBoolean(instance.upperBoundType() == BoundType.CLOSED);
      writer.writeObject(instance.upperEndpoint());
    } else {
      writer.writeBoolean(false);
    }
  }
}
