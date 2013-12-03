/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.thirdparty.publicsuffix;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Parser for a map of reversed domain names stored as a serialized radix tree.
 */
@GwtCompatible
class TrieParser {

  private static final Joiner PREFIX_JOINER = Joiner.on("");

  /**
   * Parses a serialized trie representation of a map of reversed public
   * suffixes into an immutable map of public suffixes.
   */
  static ImmutableMap<String, PublicSuffixType> parseTrie(CharSequence encoded) {
    ImmutableMap.Builder<String, PublicSuffixType> builder = ImmutableMap.builder();
    int encodedLen = encoded.length();
    int idx = 0;
    while (idx < encodedLen) {
      idx += doParseTrieToBuilder(
          Lists.<CharSequence>newLinkedList(),
          encoded.subSequence(idx, encodedLen),
          builder);
    }
    return builder.build();
  }

  /**
   * Parses a trie node and returns the number of characters consumed.
   *
   * @param stack The prefixes that preceed the characters represented by this
   *     node. Each entry of the stack is in reverse order.
   * @param encoded The serialized trie.
   * @param builder A map builder to which all entries will be added.
   * @return The number of characters consumed from {@code encoded}.
   */
  private static int doParseTrieToBuilder(
      List<CharSequence> stack,
      CharSequence encoded,
      ImmutableMap.Builder<String, PublicSuffixType> builder) {

    int encodedLen = encoded.length();
    int idx = 0;
    char c = '\0';

    // Read all of the characters for this node.
    for ( ; idx < encodedLen; idx++) {
      c = encoded.charAt(idx);
      if (c == '&' || c == '?' || c == '!' || c == ':' || c == ',') {
        break;
      }
    }

    stack.add(0, reverse(encoded.subSequence(0, idx)));

    if (c == '!' || c == '?' || c == ':' || c == ',') {
      // '!' represents an interior node that represents an ICANN entry in the map.
      // '?' represents a leaf node, which represents an ICANN entry in map.
      // ':' represents an interior node that represents a private entry in the map
      // ',' represents a leaf node, which represents a private entry in the map.
      String domain = PREFIX_JOINER.join(stack);
      if (domain.length() > 0) {
        builder.put(domain, PublicSuffixType.fromCode(c));
      }
    }
    idx++;

    if (c != '?' && c != ',') {
      while (idx < encodedLen) {
        // Read all the children
        idx += doParseTrieToBuilder(stack, encoded.subSequence(idx, encodedLen), builder);
        if (encoded.charAt(idx) == '?' || encoded.charAt(idx) == ',') {
          // An extra '?' or ',' after a child node indicates the end of all children of this node.
          idx++;
          break;
        }
      }
    }
    stack.remove(0);
    return idx;
  }

  /**
   * Reverses a character sequence. This is borrowed from
   * https://code.google.com/p/google-web-toolkit/source/detail?r=11591#
   * and can be replaced with a simple {@code StringBuffer#reverse} once GWT 2.6 is available.
   */
  private static CharSequence reverse(CharSequence s) {
    int length = s.length();
    if (length <= 1) {
      return s;
    }

    char[] buffer = new char[length];
    buffer[0] = s.charAt(length - 1);

    for (int i = 1; i < length; i++) {
      buffer[i] = s.charAt(length - 1 - i);
      if (Character.isSurrogatePair(buffer[i], buffer[i - 1])) {
        swap(buffer, i - 1, i);
      }
    }

    return new String(buffer);
  }

  private static void swap(char[] buffer, int f, int s) {
    char tmp = buffer[f];
    buffer[f] = buffer[s];
    buffer[s] = tmp;
  }
}
