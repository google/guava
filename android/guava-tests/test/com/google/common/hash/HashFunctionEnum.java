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

/**
 * An enum that contains all of the known hash functions.
 *
 * @author Kurt Alfred Kluever
 */
enum HashFunctionEnum {
  ADLER32(Hashing.adler32()),
  CRC32(Hashing.crc32()),
  GOOD_FAST_HASH_32(Hashing.goodFastHash(32)),
  GOOD_FAST_HASH_64(Hashing.goodFastHash(64)),
  GOOD_FAST_HASH_128(Hashing.goodFastHash(128)),
  GOOD_FAST_HASH_256(Hashing.goodFastHash(256)),
  MD5(Hashing.md5()),
  MURMUR3_128(Hashing.murmur3_128()),
  MURMUR3_32(Hashing.murmur3_32()),
  SHA1(Hashing.sha1()),
  SHA256(Hashing.sha256()),
  SHA384(Hashing.sha384()),
  SHA512(Hashing.sha512()),
  SIP_HASH24(Hashing.sipHash24()),
  FARMHASH_FINGERPRINT_64(Hashing.farmHashFingerprint64()),

  // Hash functions found in //javatests for comparing against current implementation of CityHash.
  // These can probably be removed sooner or later.
  ;

  private final HashFunction hashFunction;

  private HashFunctionEnum(HashFunction hashFunction) {
    this.hashFunction = hashFunction;
  }

  HashFunction getHashFunction() {
    return hashFunction;
  }
}
