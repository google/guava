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

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import java.nio.charset.Charset;

final class TestPlatform {
  static ImmutableSet<Charset> getCharsets() {
    return ImmutableSet.of(ISO_8859_1, US_ASCII, UTF_16, UTF_16BE, UTF_16LE, UTF_8);
  }

  @SuppressWarnings("deprecation") // We still need to test our deprecated APIs.
  static ImmutableMap<String, HashFunction> getAlgorithms() {
    return new ImmutableMap.Builder<String, HashFunction>()
        .put("MD5", Hashing.md5())
        .put("SHA", Hashing.sha1()) // Not the official name, but still works
        .put("SHA1", Hashing.sha1()) // Not the official name, but still works
        .put("sHa-1", Hashing.sha1()) // Not the official name, but still works
        .put("SHA-1", Hashing.sha1())
        .put("SHA-256", Hashing.sha256())
        .put("SHA-384", Hashing.sha384())
        .put("SHA-512", Hashing.sha512())
        .build();
  }

  private static final String TQBFJOTLD = "The quick brown fox jumps over the lazy dog";
  private static final String TQBFJOTLDP = "The quick brown fox jumps over the lazy dog.";

  @SuppressWarnings("deprecation") // We still need to test our deprecated APIs.
  static ImmutableTable<HashFunction, String, String> getKnownHashes() {
    return ImmutableTable.<HashFunction, String, String>builder()
        .put(Hashing.adler32(), "", "01000000")
        .put(Hashing.adler32(), TQBFJOTLD, "da0fdc5b")
        .put(Hashing.adler32(), TQBFJOTLDP, "0810e46b")
        .put(Hashing.md5(), "", "d41d8cd98f00b204e9800998ecf8427e")
        .put(Hashing.md5(), TQBFJOTLD, "9e107d9d372bb6826bd81d3542a419d6")
        .put(Hashing.md5(), TQBFJOTLDP, "e4d909c290d0fb1ca068ffaddf22cbd0")
        .put(Hashing.murmur3_128(), "", "00000000000000000000000000000000")
        .put(Hashing.murmur3_128(), TQBFJOTLD, "6c1b07bc7bbc4be347939ac4a93c437a")
        .put(Hashing.murmur3_128(), TQBFJOTLDP, "c902e99e1f4899cde7b68789a3a15d69")
        .put(Hashing.murmur3_32(), "", "00000000")
        .put(Hashing.murmur3_32(), TQBFJOTLD, "23f74f2e")
        .put(Hashing.murmur3_32(), TQBFJOTLDP, "fc8bc4d5")
        .put(Hashing.murmur3_32_fixed(), "", "00000000")
        .put(Hashing.murmur3_32_fixed(), TQBFJOTLD, "23f74f2e")
        .put(Hashing.murmur3_32_fixed(), TQBFJOTLDP, "fc8bc4d5")
        .put(Hashing.sha1(), "", "da39a3ee5e6b4b0d3255bfef95601890afd80709")
        .put(Hashing.sha1(), TQBFJOTLD, "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12")
        .put(Hashing.sha1(), TQBFJOTLDP, "408d94384216f890ff7a0c3528e8bed1e0b01621")
        .put(
            Hashing.sha256(),
            "",
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        .put(
            Hashing.sha256(),
            TQBFJOTLD,
            "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592")
        .put(
            Hashing.sha256(),
            TQBFJOTLDP,
            "ef537f25c895bfa782526529a9b63d97aa631564d5d789c2b765448c8635fb6c")
        .put(
            Hashing.sha384(),
            "",
            "38b060a751ac96384cd9327eb1b1e36a21fdb71114be07434c0cc7bf63f6e1da274edebfe76f65fbd51ad2f14898b95b")
        .put(
            Hashing.sha384(),
            TQBFJOTLD,
            "ca737f1014a48f4c0b6dd43cb177b0afd9e5169367544c494011e3317dbf9a509cb1e5dc1e85a941bbee3d7f2afbc9b1")
        .put(
            Hashing.sha384(),
            TQBFJOTLDP,
            "ed892481d8272ca6df370bf706e4d7bc1b5739fa2177aae6c50e946678718fc67a7af2819a021c2fc34e91bdb63409d7")
        .put(
            Hashing.sha512(),
            "",
            "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e")
        .put(
            Hashing.sha512(),
            TQBFJOTLD,
            "07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb642e93a252a954f23912547d1e8a3b5ed6e1bfd7097821233fa0538f3db854fee6")
        .put(
            Hashing.sha512(),
            TQBFJOTLDP,
            "91ea1245f20d46ae9a037a989f54f1f790f0a47607eeb8a14d12890cea77a1bbc6c7ed9cf205e67b7f2b8fd4c7dfd3a7a8617e45f3c463d481c7e586c39ac1ed")
        .put(Hashing.crc32(), "", "00000000")
        .put(Hashing.crc32(), TQBFJOTLD, "39a34f41")
        .put(Hashing.crc32(), TQBFJOTLDP, "e9259051")
        .put(Hashing.sipHash24(), "", "310e0edd47db6f72")
        .put(Hashing.sipHash24(), TQBFJOTLD, "e46f1fdc05612752")
        .put(Hashing.sipHash24(), TQBFJOTLDP, "9b602581fce4d4f8")
        .put(Hashing.crc32c(), "", "00000000")
        .put(Hashing.crc32c(), TQBFJOTLD, "04046222")
        .put(Hashing.crc32c(), TQBFJOTLDP, "b3970019")
        .put(Hashing.farmHashFingerprint64(), "", "4f40902f3b6ae19a")
        .put(Hashing.farmHashFingerprint64(), TQBFJOTLD, "34511b3bf383beab")
        .put(Hashing.farmHashFingerprint64(), TQBFJOTLDP, "737d7e5f8660653e")
        .put(Hashing.fingerprint2011(), "", "e365a64a907cad23")
        .put(Hashing.fingerprint2011(), TQBFJOTLD, "c9688c84e813b089")
        .put(Hashing.fingerprint2011(), TQBFJOTLDP, "a714d70f1d569cd0")
        .build();
  }

  private TestPlatform() {}
}
