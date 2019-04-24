/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.collect;

import java.util.Random;

/**
 * Utility class for being able to seed a {@link Random} value with a passed in seed from a
 * benchmark parameter.
 *
 * <p>TODO: Remove this class once Caliper has a better way.
 *
 * @author Nicholaus Shupe
 */
public final class SpecialRandom extends Random {
  public static SpecialRandom valueOf(String s) {
    return (s.length() == 0) ? new SpecialRandom() : new SpecialRandom(Long.parseLong(s));
  }

  private final boolean hasSeed;
  private final long seed;

  public SpecialRandom() {
    this.hasSeed = false;
    this.seed = 0;
  }

  public SpecialRandom(long seed) {
    super(seed);
    this.hasSeed = true;
    this.seed = seed;
  }

  @Override
  public String toString() {
    return hasSeed ? "(seed:" + seed : "(default seed)";
  }

  private static final long serialVersionUID = 0;
}
