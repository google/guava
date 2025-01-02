/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.hash;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.hash.SneakyThrows.sneakyThrow;

import com.google.errorprone.annotations.Immutable;
import com.google.j2objc.annotations.J2ObjCIncompatible;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.util.zip.Checksum;
import org.jspecify.annotations.Nullable;

/**
 * {@link HashFunction} adapter for {@link Checksum} instances.
 *
 * @author Colin Decker
 */
@Immutable
final class ChecksumHashFunction extends AbstractHashFunction implements Serializable {
  private final ImmutableSupplier<? extends Checksum> checksumSupplier;
  private final int bits;
  private final String toString;

  ChecksumHashFunction(
      ImmutableSupplier<? extends Checksum> checksumSupplier, int bits, String toString) {
    this.checksumSupplier = checkNotNull(checksumSupplier);
    checkArgument(bits == 32 || bits == 64, "bits (%s) must be either 32 or 64", bits);
    this.bits = bits;
    this.toString = checkNotNull(toString);
  }

  @Override
  public int bits() {
    return bits;
  }

  @Override
  public Hasher newHasher() {
    return new ChecksumHasher(checksumSupplier.get());
  }

  @Override
  public String toString() {
    return toString;
  }

  /** Hasher that updates a checksum. */
  private final class ChecksumHasher extends AbstractByteHasher {
    private final Checksum checksum;

    private ChecksumHasher(Checksum checksum) {
      this.checksum = checkNotNull(checksum);
    }

    @Override
    protected void update(byte b) {
      checksum.update(b);
    }

    @Override
    protected void update(byte[] bytes, int off, int len) {
      checksum.update(bytes, off, len);
    }

    @Override
    @J2ObjCIncompatible
    protected void update(ByteBuffer b) {
      if (!ChecksumMethodHandles.updateByteBuffer(checksum, b)) {
        super.update(b);
      }
    }

    @Override
    public HashCode hash() {
      long value = checksum.getValue();
      if (bits == 32) {
        /*
         * The long returned from a 32-bit Checksum will have all 0s for its second word, so the
         * cast won't lose any information and is necessary to return a HashCode of the correct
         * size.
         */
        return HashCode.fromInt((int) value);
      } else {
        return HashCode.fromLong(value);
      }
    }
  }

  @J2ObjCIncompatible
  @SuppressWarnings("unused")
  private static final class ChecksumMethodHandles {
    private static final @Nullable MethodHandle UPDATE_BB = updateByteBuffer();

    @IgnoreJRERequirement // https://github.com/mojohaus/animal-sniffer/issues/67
    static boolean updateByteBuffer(Checksum cs, ByteBuffer bb) {
      if (UPDATE_BB != null) {
        try {
          UPDATE_BB.invokeExact(cs, bb);
        } catch (Throwable e) {
          // `update` has no `throws` clause.
          sneakyThrow(e);
        }
        return true;
      } else {
        return false;
      }
    }

    private static @Nullable MethodHandle updateByteBuffer() {
      try {
        Class<?> clazz = Class.forName("java.util.zip.Checksum");
        return MethodHandles.lookup()
            .findVirtual(clazz, "update", MethodType.methodType(void.class, ByteBuffer.class));
      } catch (ClassNotFoundException e) {
        throw new AssertionError(e);
      } catch (IllegalAccessException e) {
        // That API is public.
        throw newLinkageError(e);
      } catch (NoSuchMethodException e) {
        // Only introduced in Java 9.
        return null;
      }
    }

    private static LinkageError newLinkageError(Throwable cause) {
      return new LinkageError(cause.toString(), cause);
    }
  }

  private static final long serialVersionUID = 0L;
}
