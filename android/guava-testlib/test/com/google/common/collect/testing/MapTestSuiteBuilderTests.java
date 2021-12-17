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
import com.google.common.reflect.Reflection;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
    suite.addTest(testsForSetUpTearDown());
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

  /**
   * Map generator that verifies that {@code setUp()} methods are called in all the test cases. The
   * {@code setUpRan} parameter is set true by the {@code setUp} that every test case is supposed to
   * have registered, and set false by the {@code tearDown}. We use a dynamic proxy to intercept all
   * of the {@code Map} method calls and check that {@code setUpRan} is true.
   */
  private static class CheckSetUpHashMapGenerator extends WrappedHashMapGenerator {
    private final AtomicBoolean setUpRan;

    CheckSetUpHashMapGenerator(AtomicBoolean setUpRan) {
      this.setUpRan = setUpRan;
    }

    @Override
    Map<String, String> wrap(HashMap<String, String> map) {
      @SuppressWarnings("unchecked")
      Map<String, String> proxy =
          Reflection.newProxy(Map.class, new CheckSetUpInvocationHandler(map, setUpRan));
      return proxy;
    }
  }

  /**
   * Intercepts calls to a {@code Map} to check that {@code setUpRan} is true when they happen. Then
   * forwards the calls to the underlying {@code Map}.
   */
  private static class CheckSetUpInvocationHandler implements InvocationHandler, Serializable {
    private final Map<String, String> map;
    private final AtomicBoolean setUpRan;

    CheckSetUpInvocationHandler(Map<String, String> map, AtomicBoolean setUpRan) {
      this.map = map;
      this.setUpRan = setUpRan;
    }

    @Override
    public Object invoke(Object target, Method method, Object[] args) throws Throwable {
      assertTrue("setUp should have run", setUpRan.get());
      try {
        return method.invoke(map, args);
      } catch (InvocationTargetException e) {
        throw e.getCause();
      } catch (IllegalAccessException e) {
        throw newLinkageError(e);
      }
    }
  }

  /** Verifies that {@code setUp} and {@code tearDown} are called in all map test cases. */
  private static Test testsForSetUpTearDown() {
    final AtomicBoolean setUpRan = new AtomicBoolean();
    Runnable setUp =
        new Runnable() {
          @Override
          public void run() {
            assertFalse("previous tearDown should have run before setUp", setUpRan.getAndSet(true));
          }
        };
    Runnable tearDown =
        new Runnable() {
          @Override
          public void run() {
            assertTrue("setUp should have run", setUpRan.getAndSet(false));
          }
        };
    return MapTestSuiteBuilder.using(new CheckSetUpHashMapGenerator(setUpRan))
        .named("setUpTearDown")
        .withFeatures(
            MapFeature.GENERAL_PURPOSE,
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            CollectionSize.ANY)
        .withSetUp(setUp)
        .withTearDown(tearDown)
        .createTestSuite();
  }

  private static LinkageError newLinkageError(Throwable cause) {
    LinkageError error = new LinkageError(cause.toString());
    error.initCause(cause);
    return error;
  }
}
