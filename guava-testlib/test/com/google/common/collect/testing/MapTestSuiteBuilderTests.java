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

package com.google.common.collect.testing;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.features.MapFeature;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests {@link MapTestSuiteBuilder} by using it against maps that have various negative behaviors.
 *
 * @author George van den Driessche
 */
public final class MapTestSuiteBuilderTests extends TestCase {
  private MapTestSuiteBuilderTests() {}

  public static Test suite() {
    TestSuite suite = new TestSuite(MapTestSuiteBuilderTests.class.getSimpleName());
    suite.addTest(testsForHashMapNullKeysForbidden());
    suite.addTest(testsForHashMapNullValuesForbidden());
    return suite;
  }

  private abstract static class WrappedHashMapGenerator extends TestStringMapGenerator {
    @Override
    protected final Map<String, String> create(Entry<String, String>[] entries) {
      HashMap<String, String> map = Maps.newHashMap();
      for (Entry<String, String> entry : entries) {
        map.put(entry.getKey(), entry.getValue());
      }
      return wrap(map);
    }

    abstract Map<String, String> wrap(HashMap<String, String> map);
  }

  private static TestSuite wrappedHashMapTests(
      WrappedHashMapGenerator generator, String name, Feature<?>... features) {
    List<Feature<?>> featuresList = Lists.newArrayList(features);
    Collections.addAll(
        featuresList,
        MapFeature.GENERAL_PURPOSE,
        CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
        CollectionSize.ANY);
    return MapTestSuiteBuilder.using(generator)
        .named(name)
        .withFeatures(featuresList)
        .createTestSuite();
  }

  // TODO: consider being null-hostile in these tests

  private static Test testsForHashMapNullKeysForbidden() {
    return wrappedHashMapTests(
        new WrappedHashMapGenerator() {
          @Override
          Map<String, String> wrap(final HashMap<String, String> map) {
            if (map.containsKey(null)) {
              throw new NullPointerException();
            }
            return new AbstractMap<String, String>() {
              @Override
              public Set<Entry<String, String>> entrySet() {
                return map.entrySet();
              }

              @Override
              public String put(String key, String value) {
                checkNotNull(key);
                return map.put(key, value);
              }
            };
          }
        },
        "HashMap w/out null keys",
        ALLOWS_NULL_VALUES);
  }

  private static Test testsForHashMapNullValuesForbidden() {
    return wrappedHashMapTests(
        new WrappedHashMapGenerator() {
          @Override
          Map<String, String> wrap(final HashMap<String, String> map) {
            if (map.containsValue(null)) {
              throw new NullPointerException();
            }

            return new AbstractMap<String, String>() {
              @Override
              public Set<Entry<String, String>> entrySet() {
                return new EntrySet();
              }

              @Override
              public int hashCode() {
                return map.hashCode();
              }

              @Override
              public boolean equals(Object o) {
                return map.equals(o);
              }

              @Override
              public String toString() {
                return map.toString();
              }

              @Override
              public String remove(Object key) {
                return map.remove(key);
              }

              @Override
              public boolean remove(Object key, Object value) {
                return map.remove(key, value);
              }

              class EntrySet extends AbstractSet<Map.Entry<String, String>> {
                @Override
                public Iterator<Entry<String, String>> iterator() {
                  return new Iterator<Entry<String, String>>() {

                    final Iterator<Entry<String, String>> iterator = map.entrySet().iterator();

                    @Override
                    public void remove() {
                      iterator.remove();
                    }

                    @Override
                    public boolean hasNext() {
                      return iterator.hasNext();
                    }

                    @Override
                    public Entry<String, String> next() {
                      return transform(iterator.next());
                    }

                    private Entry<String, String> transform(final Entry<String, String> next) {
                      return new Entry<String, String>() {

                        @Override
                        public String setValue(String value) {
                          checkNotNull(value);
                          return next.setValue(value);
                        }

                        @Override
                        public String getValue() {
                          return next.getValue();
                        }

                        @Override
                        public String getKey() {
                          return next.getKey();
                        }

                        @Override
                        public boolean equals(Object obj) {
                          return next.equals(obj);
                        }

                        @Override
                        public int hashCode() {
                          return next.hashCode();
                        }
                      };
                    }
                  };
                }

                @Override
                public int size() {
                  return map.size();
                }

                @Override
                public boolean remove(Object o) {
                  return map.entrySet().remove(o);
                }

                @Override
                public boolean removeIf(Predicate<? super Entry<String, String>> filter) {
                  return map.entrySet().removeIf(filter);
                }

                @Override
                public boolean containsAll(Collection<?> c) {
                  return map.entrySet().containsAll(c);
                }

                @Override
                public boolean removeAll(Collection<?> c) {
                  return map.entrySet().removeAll(c);
                }

                @Override
                public boolean retainAll(Collection<?> c) {
                  return map.entrySet().retainAll(c);
                }

                @Override
                public int hashCode() {
                  return map.entrySet().hashCode();
                }

                @Override
                public boolean equals(Object o) {
                  return map.entrySet().equals(o);
                }

                @Override
                public String toString() {
                  return map.entrySet().toString();
                }
              }

              @Override
              public String put(String key, String value) {
                checkNotNull(value);
                return map.put(key, value);
              }
            };
          }
        },
        "HashMap w/out null values",
        ALLOWS_NULL_KEYS);
  }
}
