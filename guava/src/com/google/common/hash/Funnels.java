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

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;

import java.io.OutputStream;

/**
 * Funnels for common types. All implementations are serializable.
 *
 * @author Dimitris Andreou
 * @since 11.0
 */
@Beta
public final class Funnels {
  private Funnels() {}

  /**
   * Returns a funnel that extracts the bytes from a {@code byte} array.
   */
  public static Funnel<byte[]> byteArrayFunnel() {
    return ByteArrayFunnel.INSTANCE;
  }

  private enum ByteArrayFunnel implements Funnel<byte[]> {
    INSTANCE;

    public void funnel(byte[] from, PrimitiveSink into) {
      into.putBytes(from);
    }

    @Override public String toString() {
      return "Funnels.byteArrayFunnel()";
    }
  }

  /**
   * Returns a funnel that extracts the characters from a {@code CharSequence}.
   */
  public static Funnel<CharSequence> stringFunnel() {
    return StringFunnel.INSTANCE;
  }

  private enum StringFunnel implements Funnel<CharSequence> {
    INSTANCE;

    public void funnel(CharSequence from, PrimitiveSink into) {
      into.putString(from);
    }

    @Override public String toString() {
      return "Funnels.stringFunnel()";
    }
  }
  
  /**
   * Returns a funnel for integers.
   * 
   * @since 13.0
   */
  public static Funnel<Integer> integerFunnel() {
    return IntegerFunnel.INSTANCE;
  }
  
  private enum IntegerFunnel implements Funnel<Integer> {
    INSTANCE;
    
    public void funnel(Integer from, PrimitiveSink into) {
      into.putInt(from);
    }
    
    @Override public String toString() {
      return "Funnels.integerFunnel()";
    }
  }

  /**
   * Returns a funnel for longs.
   * 
   * @since 13.0
   */
  public static Funnel<Long> longFunnel() {
    return LongFunnel.INSTANCE;
  }
  
  private enum LongFunnel implements Funnel<Long> {
    INSTANCE;
    
    public void funnel(Long from, PrimitiveSink into) {
      into.putLong(from);
    }
    
    @Override public String toString() {
      return "Funnels.longFunnel()";
    }
  }
  
  /**
   * Wraps a {@code PrimitiveSink} as an {@link OutputStream}, so it is easy to
   * {@link Funnel#funnel funnel} an object to a {@code PrimitiveSink}
   * if there is already a way to write the contents of the object to an {@code OutputStream}.  
   * 
   * <p>The {@code close} and {@code flush} methods of the returned {@code OutputStream}
   * do nothing, and no method throws {@code IOException}.
   * 
   * @since 13.0
   */
  public static OutputStream asOutputStream(PrimitiveSink sink) {
    return new SinkAsStream(sink);
  }
  
  private static class SinkAsStream extends OutputStream {
    final PrimitiveSink sink;
    SinkAsStream(PrimitiveSink sink) {
      this.sink = Preconditions.checkNotNull(sink);
    }
    
    @Override public void write(int b) {
      sink.putByte((byte) b);
    }

    @Override public void write(byte[] bytes) {
      sink.putBytes(bytes);
    }

    @Override public void write(byte[] bytes, int off, int len) {
      sink.putBytes(bytes, off, len);
    }
    
    @Override public String toString() {
      return "Funnels.asOutputStream(" + sink + ")";
    }
  }
}
