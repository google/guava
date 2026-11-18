// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import com.code_intelligence.jazzer.api.FuzzerSecurityIssueMedium;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/** Detects algorithmic-complexity hash flooding in Android-flavor immutable collections. */
public final class AndroidImmutableCollectionsHashFloodingFuzzer {
  // A magic prefix prevents ordinary random inputs from repeatedly exercising the maximum-cost
  // case. Jazzer's comparison instrumentation can still discover this structured input.
  private static final byte[] MAGIC = {'H', 'F', 'v', '1'};
  private static final long MAX_REASONABLE_EQUALS = 1_000_000L;

  private AndroidImmutableCollectionsHashFloodingFuzzer() {}

  public static void fuzzerTestOneInput(byte[] input) {
    if (input.length < MAGIC.length + 2 || !hasMagicPrefix(input)) {
      return;
    }

    int collection = Byte.toUnsignedInt(input[MAGIC.length]) % 3;
    int collisionBits = 6 + (Byte.toUnsignedInt(input[MAGIC.length + 1]) % 7);
    int count = 1 << collisionBits;

    CollisionKey.resetEqualsCalls();
    switch (collection) {
      case 0:
        ImmutableMap.Builder<CollisionKey, Integer> map =
            ImmutableMap.builderWithExpectedSize(count);
        for (int i = 0; i < count; i++) {
          map.put(new CollisionKey(i, collisionBits), i);
        }
        map.buildOrThrow();
        break;
      case 1:
        ImmutableSet.Builder<CollisionKey> set = ImmutableSet.builderWithExpectedSize(count);
        for (int i = 0; i < count; i++) {
          set.add(new CollisionKey(i, collisionBits));
        }
        set.build();
        break;
      default:
        ImmutableBiMap.Builder<CollisionKey, Integer> bimap = ImmutableBiMap.builder();
        for (int i = 0; i < count; i++) {
          bimap.put(new CollisionKey(i, collisionBits), i);
        }
        bimap.buildOrThrow();
        break;
    }

    long equalsCalls = CollisionKey.equalsCalls();
    if (equalsCalls > MAX_REASONABLE_EQUALS) {
      throw new FuzzerSecurityIssueMedium(
          "Probable hash-flooding algorithmic-complexity denial of service in "
              + collectionName(collection)
              + ": "
              + count
              + " distinct keys with one String hash required "
              + equalsCalls
              + " equals calls during construction (limit "
              + MAX_REASONABLE_EQUALS
              + ").");
    }
  }

  private static boolean hasMagicPrefix(byte[] input) {
    for (int i = 0; i < MAGIC.length; i++) {
      if (input[i] != MAGIC[i]) {
        return false;
      }
    }
    return true;
  }

  private static String collectionName(int collection) {
    switch (collection) {
      case 0:
        return "ImmutableMap";
      case 1:
        return "ImmutableSet";
      default:
        return "ImmutableBiMap";
    }
  }

  /** Comparable wrapper around distinct strings from Java's Aa/BB collision family. */
  private static final class CollisionKey implements Comparable<CollisionKey> {
    private static long equalsCalls;
    private final int id;
    private final String value;

    CollisionKey(int id, int bits) {
      this.id = id;
      StringBuilder result = new StringBuilder(bits * 2);
      for (int bit = 0; bit < bits; bit++) {
        result.append(((id >>> bit) & 1) == 0 ? "Aa" : "BB");
      }
      this.value = result.toString();
    }

    static void resetEqualsCalls() {
      equalsCalls = 0;
    }

    static long equalsCalls() {
      return equalsCalls;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      equalsCalls++;
      return other instanceof CollisionKey && value.equals(((CollisionKey) other).value);
    }

    @Override
    public int compareTo(CollisionKey other) {
      return Integer.compare(id, other.id);
    }
  }
}
