/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.thirdparty.publicsuffix;

import static com.google.common.collect.Queues.newArrayDeque;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import java.util.Deque;

/** Parser for a map of reversed domain names stored as a serialized radix tree. */
@GwtCompatible
final class TrieParser {

  private static final Joiner DIRECT_JOINER = Joiner.on("");

  /**
   * Parses a serialized trie representation of a map of reversed public suffixes into an immutable
   * map of public suffixes. The encoded trie string may be broken into multiple chunks to avoid the
   * 64k limit on string literal size. In-memory strings can be much larger (2G).
   */
  static ImmutableMap<String, PublicSuffixType> parseTrie(CharSequence... encodedChunks) {
    String encoded = DIRECT_JOINER.join(encodedChunks);
    return parseFullString(encoded);
  }

  @VisibleForTesting
  static ImmutableMap<String, PublicSuffixType> parseFullString(String encoded) {
    ImmutableMap.Builder<String, PublicSuffixType> builder = ImmutableMap.builder();
    int encodedLen = encoded.length();
    int idx = 0;

    while (idx < encodedLen) {
      idx += doParseTrieToBuilder(newArrayDeque(), encoded, idx, builder);
    }

    return builder.buildOrThrow();
  }

  /**
   * Parses a trie node and returns the number of characters consumed.
   *
   * @param stack The prefixes that precede the characters represented by this node. Each entry of
   *     the stack is in reverse order.
   * @param encoded The serialized trie.
   * @param start An index in the encoded serialized trie to begin reading characters from.
   * @param builder A map builder to which all entries will be added.
   * @return The number of characters consumed from {@code encoded}.
   */
  private static int doParseTrieToBuilder(
      Deque<CharSequence> stack,
      CharSequence encoded,
      int start,
      ImmutableMap.Builder<String, PublicSuffixType> builder) {

    int encodedLen = encoded.length();
    int idx = start;
    char c = '\0';

    // Read all the characters for this node.
    for (; idx < encodedLen; idx++) {
      c = encoded.charAt(idx);

      if (c == '&' || c == '?' || c == '!' || c == ':' || c == ',') {
        break;
      }
    }

    stack.push(reverse(encoded.subSequence(start, idx)));

    if (c == '!' || c == '?' || c == ':' || c == ',') {
      // '!' represents an interior node that represents a REGISTRY entry in the map.
      // '?' represents a leaf node, which represents a REGISTRY entry in map.
      // ':' represents an interior node that represents a private entry in the map
      // ',' represents a leaf node, which represents a private entry in the map.
      String domain = DIRECT_JOINER.join(stack);

      if (domain.length() > 0) {
        builder.put(domain, PublicSuffixType.fromCode(c));
      }
    }

    idx++;

    if (c != '?' && c != ',') {
      while (idx < encodedLen) {
        // Read all the children
        idx += doParseTrieToBuilder(stack, encoded, idx, builder);

        if (encoded.charAt(idx) == '?' || encoded.charAt(idx) == ',') {
          // An extra '?' or ',' after a child node indicates the end of all children of this node.
          idx++;
          break;
        }
      }
    }

    stack.pop();
    return idx - start;
  }

  private static CharSequence reverse(CharSequence s) {
    return new StringBuilder(s).reverse();
  }
}
