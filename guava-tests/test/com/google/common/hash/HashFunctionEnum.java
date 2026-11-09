/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common.hash;

import static com.google.common.hash.Hashing.adler32;
import static com.google.common.hash.Hashing.crc32;
import static com.google.common.hash.Hashing.farmHashFingerprint64;
import static com.google.common.hash.Hashing.goodFastHash;
import static com.google.common.hash.Hashing.md5;
import static com.google.common.hash.Hashing.murmur3_128;
import static com.google.common.hash.Hashing.murmur3_32;
import static com.google.common.hash.Hashing.murmur3_32_fixed;
import static com.google.common.hash.Hashing.sha1;
import static com.google.common.hash.Hashing.sha256;
import static com.google.common.hash.Hashing.sha384;
import static com.google.common.hash.Hashing.sha512;
import static com.google.common.hash.Hashing.sipHash24;

import com.google.common.annotations.J2ktIncompatible;
import org.jspecify.annotations.NullUnmarked;

/**
 * An enum that contains all of the known hash functions.
 *
 * @author Kurt Alfred Kluever
 */
@SuppressWarnings("deprecation") // We still need to test our deprecated APIs.
@NullUnmarked
@J2ktIncompatible
// Public so JMH-generated harness classes in com.google.common.hash.jmh_generated can access it.
public enum HashFunctionEnum {
  ADLER32(adler32()),
  CRC32(crc32()),
  GOOD_FAST_HASH_32(goodFastHash(32)),
  GOOD_FAST_HASH_64(goodFastHash(64)),
  GOOD_FAST_HASH_128(goodFastHash(128)),
  GOOD_FAST_HASH_256(goodFastHash(256)),
  MD5(md5()),
  MURMUR3_128(murmur3_128()),
  MURMUR3_32(murmur3_32()),
  MURMUR3_32_FIXED(murmur3_32_fixed()),
  SHA1(sha1()),
  SHA256(sha256()),
  SHA384(sha384()),
  SHA512(sha512()),
  SIP_HASH24(sipHash24()),
  FARMHASH_FINGERPRINT_64(farmHashFingerprint64()),

  // Hash functions found in //javatests for comparing against current implementation of CityHash.
  // These can probably be removed sooner or later.
  ;

  private final HashFunction hashFunction;

  HashFunctionEnum(HashFunction hashFunction) {
    this.hashFunction = hashFunction;
  }

  HashFunction getHashFunction() {
    return hashFunction;
  }
}
