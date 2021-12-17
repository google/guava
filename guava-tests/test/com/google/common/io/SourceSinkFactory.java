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

package com.google.common.io;

import java.io.File;
import java.io.IOException;

/**
 * A test factory for byte or char sources or sinks. In addition to creating sources or sinks, the
 * factory specifies what content should be expected to be read from a source or contained in a sink
 * given the content data that was used to create the source or that was written to the sink.
 *
 * <p>A single {@code SourceSinkFactory} implementation generally corresponds to one specific way of
 * creating a source or sink, such as {@link Files#asByteSource(File)}. Implementations of {@code
 * SourceSinkFactory} for common.io are found in {@link SourceSinkFactories}.
 *
 * @param <S> the source or sink type
 * @param <T> the data type (byte[] or String)
 * @author Colin Decker
 */
public interface SourceSinkFactory<S, T> {

  /**
   * Returns the data to expect the source or sink to contain given the data that was used to create
   * the source or written to the sink. Typically, this will just return the input directly, but in
   * some cases it may alter the input. For example, if the factory returns a sliced view of a
   * source created with some given bytes, this method would return a subsequence of the given
   * (byte[]) data.
   */
  T getExpected(T data);

  /** Cleans up anything created when creating the source or sink. */
  public abstract void tearDown() throws IOException;

  /** Factory for byte or char sources. */
  public interface SourceFactory<S, T> extends SourceSinkFactory<S, T> {

    /** Creates a new source containing some or all of the given data. */
    S createSource(T data) throws IOException;
  }

  /** Factory for byte or char sinks. */
  public interface SinkFactory<S, T> extends SourceSinkFactory<S, T> {

    /** Creates a new sink. */
    S createSink() throws IOException;

    /** Gets the current content of the created sink. */
    T getSinkContents() throws IOException;
  }

  /** Factory for {@link ByteSource} instances. */
  public interface ByteSourceFactory extends SourceFactory<ByteSource, byte[]> {}

  /** Factory for {@link ByteSink} instances. */
  public interface ByteSinkFactory extends SinkFactory<ByteSink, byte[]> {}

  /** Factory for {@link CharSource} instances. */
  public interface CharSourceFactory extends SourceFactory<CharSource, String> {}

  /** Factory for {@link CharSink} instances. */
  public interface CharSinkFactory extends SinkFactory<CharSink, String> {}
}
