/*
 * Copyright (C) 2011 The Guava Authors
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

import com.google.errorprone.annotations.Immutable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An abstract composition of multiple hash functions. {@linkplain #newHasher()} delegates to the
 * {@code Hasher} objects of the delegate hash functions, and in the end, they are used by
 * {@linkplain #makeHash(Hasher[])} that constructs the final {@code HashCode}.
 *
 * @author Dimitris Andreou
 */
@Immutable
@ElementTypesAreNonnullByDefault
abstract class AbstractCompositeHashFunction extends AbstractHashFunction {

  @SuppressWarnings("Immutable") // array not modified after creation
  final HashFunction[] functions;

  AbstractCompositeHashFunction(HashFunction... functions) {
    for (HashFunction function : functions) {
      checkNotNull(function);
    }
    this.functions = functions;
  }

  /**
   * Constructs a {@code HashCode} from the {@code Hasher} objects of the functions. Each of them
   * has consumed the entire input and they are ready to output a {@code HashCode}. The order of the
   * hashers are the same order as the functions given to the constructor.
   */
  // this could be cleaner if it passed HashCode[], but that would create yet another array...
  /* protected */ abstract HashCode makeHash(Hasher[] hashers);

  @Override
  public Hasher newHasher() {
    Hasher[] hashers = new Hasher[functions.length];
    for (int i = 0; i < hashers.length; i++) {
      hashers[i] = functions[i].newHasher();
    }
    return fromHashers(hashers);
  }

  @Override
  public Hasher newHasher(int expectedInputSize) {
    checkArgument(expectedInputSize >= 0);
    Hasher[] hashers = new Hasher[functions.length];
    for (int i = 0; i < hashers.length; i++) {
      hashers[i] = functions[i].newHasher(expectedInputSize);
    }
    return fromHashers(hashers);
  }

  private Hasher fromHashers(final Hasher[] hashers) {
    return new Hasher() {
      @Override
      public Hasher putByte(byte b) {
        for (Hasher hasher : hashers) {
          hasher.putByte(b);
        }
        return this;
      }

      @Override
      public Hasher putBytes(byte[] bytes) {
        for (Hasher hasher : hashers) {
          hasher.putBytes(bytes);
        }
        return this;
      }

      @Override
      public Hasher putBytes(byte[] bytes, int off, int len) {
        for (Hasher hasher : hashers) {
          hasher.putBytes(bytes, off, len);
        }
        return this;
      }

      @Override
      public Hasher putBytes(ByteBuffer bytes) {
        int pos = bytes.position();
        for (Hasher hasher : hashers) {
          Java8Compatibility.position(bytes, pos);
          hasher.putBytes(bytes);
        }
        return this;
      }

      @Override
      public Hasher putShort(short s) {
        for (Hasher hasher : hashers) {
          hasher.putShort(s);
        }
        return this;
      }

      @Override
      public Hasher putInt(int i) {
        for (Hasher hasher : hashers) {
          hasher.putInt(i);
        }
        return this;
      }

      @Override
      public Hasher putLong(long l) {
        for (Hasher hasher : hashers) {
          hasher.putLong(l);
        }
        return this;
      }

      @Override
      public Hasher putFloat(float f) {
        for (Hasher hasher : hashers) {
          hasher.putFloat(f);
        }
        return this;
      }

      @Override
      public Hasher putDouble(double d) {
        for (Hasher hasher : hashers) {
          hasher.putDouble(d);
        }
        return this;
      }

      @Override
      public Hasher putBoolean(boolean b) {
        for (Hasher hasher : hashers) {
          hasher.putBoolean(b);
        }
        return this;
      }

      @Override
      public Hasher putChar(char c) {
        for (Hasher hasher : hashers) {
          hasher.putChar(c);
        }
        return this;
      }

      @Override
      public Hasher putUnencodedChars(CharSequence chars) {
        for (Hasher hasher : hashers) {
          hasher.putUnencodedChars(chars);
        }
        return this;
      }

      @Override
      public Hasher putString(CharSequence chars, Charset charset) {
        for (Hasher hasher : hashers) {
          hasher.putString(chars, charset);
        }
        return this;
      }

      @Override
      public <T extends @Nullable Object> Hasher putObject(
          @ParametricNullness T instance, Funnel<? super T> funnel) {
        for (Hasher hasher : hashers) {
          hasher.putObject(instance, funnel);
        }
        return this;
      }

      @Override
      public HashCode hash() {
        return makeHash(hashers);
      }
    };
  }

  private static final long serialVersionUID = 0L;
}
