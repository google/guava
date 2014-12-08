/*
 * Copyright (C) 2014 The Guava Authors
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

package com.google.common.net;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.Benchmark;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Random;

/**
 * Compares performance of an old implementation of {@link UriParameterMap#parse(String, Charset)},
 * with the current implementation.
 */
public class UriParameterMapBenchmark {
  private static final int NUMBER_OF_RANDOM_PARAMETER_STRINGS = 1024;
  private static final int MAX_PARAMS_IN_RANDOM_STRING = 10;
  private static final double VALUE_ODDS = 0.7;
  private static final String[] PARAM_NAMES = {"n1", "name2", "name3", "name4", "name5", "name6",
     "name7", "longname8", "longername9", "veryVeryLongName10"};
  private static final String[] PARAM_VALUES = {"v1", "value2", "value3", "value%204",
     "longValueName5", "very%20Very%20Long%Name6",
     "ABCDEFGHIJKLMNOPQRSTUVWXYZ%20abcdefghijklmnopqrstuvwxyz%200123456789"};

  private static String[] randomParams = generateAllRandomParams();

  private static String[] generateAllRandomParams() {
    String[] params = new String[NUMBER_OF_RANDOM_PARAMETER_STRINGS];
    Random random = new Random();
    randomParams = new String[NUMBER_OF_RANDOM_PARAMETER_STRINGS];
    for (int i = 0; i < NUMBER_OF_RANDOM_PARAMETER_STRINGS; i++) {
      params[i] = generateRandomParams(random);
    }
    return params;
  }

  private static String generateRandomParams(Random random) {    
    StringBuilder sb = new StringBuilder();
    int numParameters = random.nextInt(MAX_PARAMS_IN_RANDOM_STRING - 1) + 1;
    for (int i = 0; i < numParameters; i++) {
      sb.append(PARAM_NAMES[random.nextInt(PARAM_NAMES.length)]);
      if (random.nextDouble() < VALUE_ODDS) {
        sb.append('=');
        sb.append(PARAM_VALUES[random.nextInt(PARAM_VALUES.length)]);
      }
      if (i < (numParameters - 1)) {
        sb.append('&');
      }
    }
    return sb.toString();
  }

  @Benchmark boolean legacyImplRandom(int reps) {
    // Paranoia: acting on hearsay that accessing fields might be slow.
    String[] randomParams = this.randomParams;
    boolean created = false;
    // Allows us to use & instead of %, acting on hearsay that division
    // operators (/%) are disproportionately expensive.
    int mask = NUMBER_OF_RANDOM_PARAMETER_STRINGS - 1;

    for (int i = 0; i < reps; i++) {
      UriParameterMap map = legacyParse(randomParams[i & mask], Charsets.UTF_8);
      created ^= (map != null);
    }
    return created;
  }

  @Benchmark boolean currentImplRandom(int reps) {
    // Paranoia: acting on hearsay that accessing fields might be slow.
    String[] randomParams = this.randomParams;
    boolean created = false;
    // Allows us to use & instead of %, acting on hearsay that division
    // operators (/%) are disproportionately expensive.
    int mask = NUMBER_OF_RANDOM_PARAMETER_STRINGS - 1;

    for (int i = 0; i < reps; i++) {
      UriParameterMap map = UriParameterMap.parse(randomParams[i & mask], Charsets.UTF_8);
      created ^= (map != null);
    }
    return created;
  }

  /**
   * This is an old implementation of {@link UriParameterMap#parse(String, Charset)},
   * which used {@link Splitter}, String#split(String, int), String#toUpperCase() and
   * String#endsWith(String).
   */
  private static UriParameterMap legacyParse(String query, Charset encoding) {
    checkNotNull(query);
    checkNotNull(encoding);
    UriParameterMap map = new UriParameterMap();
    if (!query.isEmpty()) {
      Iterable<String> pieces = Splitter.on('&').split(query);
      int count = 0;
      for (String piece : pieces) {
        count++;
        if (count > maxUriParameterCount) {
          throw new TooManyUriParametersException();
        }
        String[] pair = piece.split("=", 2);
        String name = decodeString(pair[0], encoding);
        String value = pair.length < 2 ? "" : decodeString(pair[1], encoding);
        map.put(name, value);
      }
    }
    return map;
  }

  private static String decodeString(String str, Charset encoding) {
    try {
      if (requiresByteLevelDecoding(encoding)) {
        byte[] rawBytes = URLDecoder.decode(str, "ISO-8859-1")
            .getBytes(Charsets.ISO_8859_1);
        return new String(rawBytes, encoding);
      }
      return URLDecoder.decode(str, encoding.name());
    } catch (IllegalArgumentException iae) {
      return str;
    } catch (UnsupportedEncodingException e) {
      return str;
    }
  }

  private static boolean requiresByteLevelDecoding(Charset charset) {
    String encoding = charset.name().toUpperCase();
    // Use endsWith() to include our wrapper character sets, whose names are
    // of the form "X-Variant-Shift_JIS" or "X-Variant-windows-31j".
    return (encoding.endsWith("SHIFT_JIS") || encoding.endsWith("WINDOWS-31J"));
  }

  private static int maxUriParameterCount = 512;
}
