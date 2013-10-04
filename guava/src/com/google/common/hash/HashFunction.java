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
import com.google.common.primitives.Ints;

import java.nio.charset.Charset;

/**
 * A hash function is a collision-averse pure function that maps an arbitrary block of
 * data to a number called a <i>hash code</i>.
 *
 * <h3>Definition</h3>
 *
 * <p>Unpacking this definition:
 *
 * <ul>
 * <li><b>block of data:</b> the input for a hash function is always, in concept, an
 *     ordered byte array. This hashing API accepts an arbitrary sequence of byte and
 *     multibyte values (via {@link Hasher}), but this is merely a convenience; these are
 *     always translated into raw byte sequences under the covers.
 *
 * <li><b>hash code:</b> each hash function always yields hash codes of the same fixed bit
 *     length (given by {@link #bits}). For example, {@link Hashing#sha1} produces a
 *     160-bit number, while {@link Hashing#murmur3_32()} yields only 32 bits. Because a
 *     {@code long} value is clearly insufficient to hold all hash code values, this API
 *     represents a hash code as an instance of {@link HashCode}.
 *
 * <li><b>pure function:</b> the value produced must depend only on the input bytes, in
 *     the order they appear. Input data is never modified. {@link HashFunction} instances
 *     should always be stateless, and therefore thread-safe.
 *
 * <li><b>collision-averse:</b> while it can't be helped that a hash function will
 *     sometimes produce the same hash code for distinct inputs (a "collision"), every
 *     hash function strives to <i>some</i> degree to make this unlikely. (Without this
 *     condition, a function that always returns zero could be called a hash function. It
 *     is not.)
 * </ul>
 *
 * <p>Summarizing the last two points: "equal yield equal <i>always</i>; unequal yield
 * unequal <i>often</i>." This is the most important characteristic of all hash functions.
 *
 * <h3>Desirable properties</h3>
 *
 * <p>A high-quality hash function strives for some subset of the following virtues:
 *
 * <ul>
 * <li><b>collision-resistant:</b> while the definition above requires making at least
 *     <i>some</i> token attempt, one measure of the quality of a hash function is <i>how
 *     well</i> it succeeds at this goal. Important note: it may be easy to achieve the
 *     theoretical minimum collision rate when using completely <i>random</i> sample
 *     input. The true test of a hash function is how it performs on representative
 *     real-world data, which tends to contain many hidden patterns and clumps. The goal
 *     of a good hash function is to stamp these patterns out as thoroughly as possible.
 *
 * <li><b>bit-dispersing:</b> masking out any <i>single bit</i> from a hash code should
 *     yield only the expected <i>twofold</i> increase to all collision rates. Informally,
 *     the "information" in the hash code should be as evenly "spread out" through the
 *     hash code's bits as possible. The result is that, for example, when choosing a
 *     bucket in a hash table of size 2^8, <i>any</i> eight bits could be consistently
 *     used.
 *
 * <li><b>cryptographic:</b> certain hash functions such as {@link Hashing#sha512} are
 *     designed to make it as infeasible as possible to reverse-engineer the input that
 *     produced a given hash code, or even to discover <i>any</i> two distinct inputs that
 *     yield the same result. These are called <i>cryptographic hash functions</i>. But,
 *     whenever it is learned that either of these feats has become computationally
 *     feasible, the function is deemed "broken" and should no longer be used for secure
 *     purposes. (This is the likely eventual fate of <i>all</i> cryptographic hashes.)
 *
 * <li><b>fast:</b> perhaps self-explanatory, but often the most important consideration.
 *     We have published <a href="#noWeHaventYet">microbenchmark results</a> for many
 *     common hash functions.
 * </ul>
 *
 * <h3>Providing input to a hash function</h3>
 *
 * <p>The primary way to provide the data that your hash function should act on is via a
 * {@link Hasher}. Obtain a new hasher from the hash function using {@link #newHasher},
 * "push" the relevant data into it using methods like {@link Hasher#putBytes(byte[])},
 * and finally ask for the {@code HashCode} when finished using {@link Hasher#hash}. (See
 * an {@linkplain #newHasher example} of this.)
 *
 * <p>If all you want to hash is a single byte array, string or {@code long} value, there
 * are convenient shortcut methods defined directly on {@link HashFunction} to make this
 * easier.
 *
 * <p>Hasher accepts primitive data types, but can also accept any Object of type {@code
 * T} provided that you implement a {@link Funnel Funnel<T>} to specify how to "feed" data
 * from that object into the function. (See {@linkplain Hasher#putObject an example} of
 * this.)
 *
 * <p><b>Compatibility note:</b> Throughout this API, multibyte values are always
 * interpreted in <i>little-endian</i> order. That is, hashing the byte array {@code
 * {0x01, 0x02, 0x03, 0x04}} is equivalent to hashing the {@code int} value {@code
 * 0x04030201}. If this isn't what you need, methods such as {@link Integer#reverseBytes}
 * and {@link Ints#toByteArray} will help.
 *
 * <h3>Relationship to {@link Object#hashCode}</h3>
 *
 * <p>Java's baked-in concept of hash codes is constrained to 32 bits, and provides no
 * separation between hash algorithms and the data they act on, so alternate hash
 * algorithms can't be easily substituted. Also, implementations of {@code hashCode} tend
 * to be poor-quality, in part because they end up depending on <i>other</i> existing
 * poor-quality {@code hashCode} implementations, including those in many JDK classes.
 *
 * <p>{@code Object.hashCode} implementations tend to be very fast, but have weak
 * collision prevention and <i>no</i> expectation of bit dispersion. This leaves them
 * perfectly suitable for use in hash tables, because extra collisions cause only a slight
 * performance hit, while poor bit dispersion is easily corrected using a secondary hash
 * function (which all reasonable hash table implementations in Java use). For the many
 * uses of hash functions beyond data structures, however, {@code Object.hashCode} almost
 * always falls short -- hence this library.
 *
 * @author Kevin Bourrillion
 * @since 11.0
 */
@Beta
public interface HashFunction {
  /**
   * Begins a new hash code computation by returning an initialized, stateful {@code
   * Hasher} instance that is ready to receive data. Example: <pre>   {@code
   *
   *   HashFunction hf = Hashing.md5();
   *   HashCode hc = hf.newHasher()
   *       .putLong(id)
   *       .putBoolean(isActive)
   *       .hash();}</pre>
   */
  Hasher newHasher();

  /**
   * Begins a new hash code computation as {@link #newHasher()}, but provides a hint of the
   * expected size of the input (in bytes). This is only important for non-streaming hash
   * functions (hash functions that need to buffer their whole input before processing any
   * of it).
   */
  Hasher newHasher(int expectedInputSize);

  /**
   * Shortcut for {@code newHasher().putInt(input).hash()}; returns the hash code for the given
   * {@code int} value, interpreted in little-endian byte order. The implementation <i>might</i>
   * perform better than its longhand equivalent, but should not perform worse.
   *
   * @since 12.0
   */
  HashCode hashInt(int input);

  /**
   * Shortcut for {@code newHasher().putLong(input).hash()}; returns the hash code for the
   * given {@code long} value, interpreted in little-endian byte order. The implementation
   * <i>might</i> perform better than its longhand equivalent, but should not perform worse.
   */
  HashCode hashLong(long input);

  /**
   * Shortcut for {@code newHasher().putBytes(input).hash()}. The implementation
   * <i>might</i> perform better than its longhand equivalent, but should not perform
   * worse.
   */
  HashCode hashBytes(byte[] input);

  /**
   * Shortcut for {@code newHasher().putBytes(input, off, len).hash()}. The implementation
   * <i>might</i> perform better than its longhand equivalent, but should not perform
   * worse.
   *
   * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > bytes.length}
   *   or {@code len < 0}
   */
  HashCode hashBytes(byte[] input, int off, int len);

  /**
   * Shortcut for {@code newHasher().putUnencodedChars(input).hash()}. The implementation
   * <i>might</i> perform better than its longhand equivalent, but should not perform worse.
   * Note that no character encoding is performed; the low byte and high byte of each {@code char}
   * are hashed directly (in that order).
   *
   * @since 15.0 (since 11.0 as hashString(CharSequence)).
   */
  HashCode hashUnencodedChars(CharSequence input);

  /**
   * Shortcut for {@code newHasher().putString(input, charset).hash()}. Characters are encoded
   * using the given {@link Charset}. The implementation <i>might</i> perform better than its
   * longhand equivalent, but should not perform worse.
   */
  HashCode hashString(CharSequence input, Charset charset);

  /**
   * Shortcut for {@code newHasher().putObject(instance, funnel).hash()}. The implementation
   * <i>might</i> perform better than its longhand equivalent, but should not perform worse.
   *
   * @since 14.0
   */
  <T> HashCode hashObject(T instance, Funnel<? super T> funnel);

  /**
   * Returns the number of bits (a multiple of 32) that each hash code produced by this
   * hash function has.
   */
  int bits();
}
