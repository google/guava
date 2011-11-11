/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.testing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Tests serialization and deserialization of an object, optionally asserting
 * that the resulting object is equal to the original.
 *
 *
 * @author Mike Bostock
 * @since 10.0
 */
@Beta
public final class SerializableTester {
  private SerializableTester() {}

  /**
   * Serializes and deserializes the specified object.
   *
   * <p>Note that the specified object may not be known by the compiler to be a
   * {@link java.io.Serializable} instance, and is thus declared an
   * {@code Object}. For example, it might be declared as a {@code List}.
   *
   * @return the re-serialized object
   * @throws RuntimeException if the specified object was not successfully
   *     serialized or deserialized
   */
  @SuppressWarnings("unchecked")
  public static <T> T reserialize(T object) {
    checkNotNull(object);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try {
      ObjectOutputStream out = new ObjectOutputStream(bytes);
      out.writeObject(object);
      ObjectInputStream in = new ObjectInputStream(
          new ByteArrayInputStream(bytes.toByteArray()));
      return (T) in.readObject();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Serializes and deserializes the specified object and verifies that the
   * re-serialized object is equal to the provided object, that the hashcodes
   * are identical, and that the class of the re-serialized object is identical
   * to that of the original.
   *
   * <p>Note that the specified object may not be known by the compiler to be a
   * {@link java.io.Serializable} instance, and is thus declared an
   * {@code Object}. For example, it might be declared as a {@code List}.
   *
   * <p>Note also that serialization is not in general required to return an
   * object that is {@linkplain Object#equals equal} to the original, nor is it
   * required to return even an object of the same class. For example, if
   * sublists of {@code MyList} instances were serializable, those sublists
   * might implement a private {@code MySubList} type but serialize as a plain
   * {@code MyList} to save space. So long as {@code MyList} has all the public
   * supertypes of {@code MySubList}, this is safe. For these cases, for which
   * {@code reserializeAndAssert} is too strict, use {@link #reserialize}.
   *
   * @return the re-serialized object
   * @throws RuntimeException if the specified object was not successfully
   *     serialized or deserialized
   * @throws GuavaAsserts.TestAssertionFailure if the re-serialized object is
   *     not equal to the original object, or if the hashcodes are different.
   */
  public static <T> T reserializeAndAssert(T object) {
    T copy = reserialize(object);
    new EqualsTester()
        .addEqualityGroup(object, copy)
        .testEquals();
    GuavaAsserts.assertEquals(object.getClass(), copy.getClass());
    return copy;
  }
}
