/*
 * Copyright (C) 2017 The Guava Authors
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

package com.google.common.io;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.VmOptions;
import com.google.common.base.Optional;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Random;

/**
 * Benchmarks for various potential implementations of {@code ByteSource.asCharSource(...).read()}.
 */
// These benchmarks allocate a lot of data so use a large heap
@VmOptions({"-Xms12g", "-Xmx12g", "-d64"})
public class ByteSourceAsCharSourceReadBenchmark {
  enum ReadStrategy {
    TO_BYTE_ARRAY_NEW_STRING {
      @Override
      String read(ByteSource byteSource, Charset cs) throws IOException {
        return new String(byteSource.read(), cs);
      }
    },
    USING_CHARSTREAMS_COPY {
      @Override
      String read(ByteSource byteSource, Charset cs) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(byteSource.openStream(), cs)) {
          CharStreams.copy(reader, sb);
        }
        return sb.toString();
      }
    },
    // It really seems like this should be faster than TO_BYTE_ARRAY_NEW_STRING.  But it just isn't
    // my best guess is that the jdk authors have spent more time optimizing that callpath than this
    // one. (StringCoding$StringDecoder vs. StreamDecoder).  StringCoding has a ton of special cases
    // theoretically we could duplicate all that logic here to try to beat 'new String' or at least
    // come close.
    USING_DECODER_WITH_SIZE_HINT {
      @Override
      String read(ByteSource byteSource, Charset cs) throws IOException {
        Optional<Long> size = byteSource.sizeIfKnown();
        // if we know the size and it fits in an int
        if (size.isPresent() && size.get().longValue() == size.get().intValue()) {
          // otherwise try to presize a StringBuilder
          // it is kind of lame that we need to construct a decoder to access this value.
          // if this is a concern we could add special cases for some known charsets (like utf8)
          // or we could avoid inputstreamreader and use the decoder api directly
          // TODO(lukes): in a real implementation we would need to handle overflow conditions
          int maxChars = (int) (size.get().intValue() * cs.newDecoder().maxCharsPerByte());
          char[] buffer = new char[maxChars];
          int bufIndex = 0;
          int remaining = buffer.length;
          try (InputStreamReader reader = new InputStreamReader(byteSource.openStream(), cs)) {
            int nRead = 0;
            while (remaining > 0 && (nRead = reader.read(buffer, bufIndex, remaining)) != -1) {
              bufIndex += nRead;
              remaining -= nRead;
            }
            if (nRead == -1) {
              // we reached EOF
              return new String(buffer, 0, bufIndex);
            }
            // otherwise we got the size wrong.  This can happen if the size changes between when
            // we called sizeIfKnown and when we started reading the file (or i guess if
            // maxCharsPerByte is wrong)
            // Fallback to an incremental approach
            StringBuilder builder = new StringBuilder(bufIndex + 32);
            builder.append(buffer, 0, bufIndex);
            buffer = null; // release for gc
            CharStreams.copy(reader, builder);
            return builder.toString();
          }

        } else {
          return TO_BYTE_ARRAY_NEW_STRING.read(byteSource, cs);
        }
      }
    };

    abstract String read(ByteSource byteSource, Charset cs) throws IOException;
  }

  @Param({"UTF-8"})
  String charsetName;

  @Param ReadStrategy strategy;

  @Param({"10", "1024", "1048576"})
  int size;

  Charset charset;
  ByteSource data;

  @BeforeExperiment
  public void setUp() {
    charset = Charset.forName(charsetName);
    StringBuilder sb = new StringBuilder();
    Random random = new Random(0xdeadbeef); // for unpredictable but reproducible behavior
    sb.ensureCapacity(size);
    for (int k = 0; k < size; k++) {
      // [9-127) includes all ascii non-control characters
      sb.append((char) (random.nextInt(127 - 9) + 9));
    }
    String string = sb.toString();
    sb.setLength(0);
    data = ByteSource.wrap(string.getBytes(charset));
  }

  @Benchmark
  public int timeCopy(int reps) throws IOException {
    int r = 0;
    final Charset localCharset = charset;
    final ByteSource localData = data;
    final ReadStrategy localStrategy = strategy;
    for (int i = 0; i < reps; i++) {
      r += localStrategy.read(localData, localCharset).hashCode();
    }
    return r;
  }
}
