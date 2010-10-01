/*
 * Copyright (C) 2010 Google Inc.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.google.common.annotations.Beta;

/**
 * Helper class for testing whether a class is serializable.
 *
 * <p>Unlike {@link com.google.common.util.SerializationChecker}, this class
 * tests not only whether serialization succeeds, but also whether the
 * serialized form is <i>correct</i>: i.e., whether an equivalent object can be
 * reconstructed by <i>deserializing</i> the serialized form.
 *
 * <p>If serialization fails, you can use {@code SerializationChecker} to
 * diagnose which referenced fields were not serializable.
 *
 * @see java.io.Serializable
 * @author mbostock@google.com (Mike Bostock)
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
   * @throws SerializationException if the specified object was not successfully
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
    } catch (RuntimeException e) {
      throw new SerializationException(e);
    } catch (IOException e) {
      throw new SerializationException(e);
    } catch (ClassNotFoundException e) {
      throw new SerializationException(e);
    }
  }

  /**
   * Serializes and deserializes the specified object and verifies that the
   * re-serialized object is equal to the provided object, and that the
   * hashcodes are identical.
   *
   * <p>Note that the specified object may not be known by the compiler to be a
   * {@link java.io.Serializable} instance, and is thus declared an
   * {@code Object}. For example, it might be declared as a {@code List}.
   *
   * @return the re-serialized object
   * @throws SerializationException if the specified object was not successfully
   *     serialized or deserialized
   * @throws junit.framework.AssertionFailedError if the re-serialized
   *     object is not equal to the original object, or if the hashcodes
   *     are different.
   */
  public static <T> T reserializeAndAssert(T object) {
    T copy = reserialize(object);
    MoreAsserts.checkEqualsAndHashCodeMethods(
        "Equals/Hashcode mismatch.  original=" + object + ", copy=" + copy,
        object, copy, true);
    return copy;
  }
}