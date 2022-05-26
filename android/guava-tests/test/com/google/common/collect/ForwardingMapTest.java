/*
 * Copyright (C) 2009 The Guava Authors
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

import static java.lang.reflect.Modifier.STATIC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.common.base.Function;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.Reflection;
import com.google.common.reflect.TypeToken;
import com.google.common.testing.ArbitraryInstances;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.ForwardingWrapperTester;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for {@link ForwardingMap}.
 *
 * @author Hayward Chan
 * @author Louis Wasserman
 */
public class ForwardingMapTest extends TestCase {
  static class StandardImplForwardingMap<K, V> extends ForwardingMap<K, V> {
    private final Map<K, V> backingMap;

    StandardImplForwardingMap(Map<K, V> backingMap) {
      this.backingMap = backingMap;
    }

    @Override
    protected Map<K, V> delegate() {
      return backingMap;
    }

    @Override
    public boolean containsKey(Object key) {
      return standardContainsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      return standardContainsValue(value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
      standardPutAll(map);
    }

    @Override
    public V remove(Object object) {
      return standardRemove(object);
    }

    @Override
    public boolean equals(Object object) {
      return standardEquals(object);
    }

    @Override
    public int hashCode() {
      return standardHashCode();
    }

    @Override
    public Set<K> keySet() {
      return new StandardKeySet();
    }

    @Override
    public Collection<V> values() {
      return new StandardValues();
    }

    @Override
    public String toString() {
      return standardToString();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      return new StandardEntrySet() {
        @Override
        public Iterator<Entry<K, V>> iterator() {
          return delegate().entrySet().iterator();
        }
      };
    }

    @Override
    public void clear() {
      standardClear();
    }

    @Override
    public boolean isEmpty() {
      return standardIsEmpty();
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTestSuite(ForwardingMapTest.class);
    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestStringMapGenerator() {

                  @Override
                  protected Map<String, String> create(Entry<String, String>[] entries) {
                    Map<String, String> map = Maps.newLinkedHashMap();
                    for (Entry<String, String> entry : entries) {
                      map.put(entry.getKey(), entry.getValue());
                    }
                    return new StandardImplForwardingMap<>(map);
                  }
                })
            .named("ForwardingMap[LinkedHashMap] with standard implementations")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.KNOWN_ORDER)
            .createTestSuite());
    suite.addTest(
        MapTestSuiteBuilder.using(
                new TestStringMapGenerator() {

                  @Override
                  protected Map<String, String> create(Entry<String, String>[] entries) {
                    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
                    for (Entry<String, String> entry : entries) {
                      builder.put(entry.getKey(), entry.getValue());
                    }
                    return new StandardImplForwardingMap<>(builder.buildOrThrow());
                  }
                })
            .named("ForwardingMap[ImmutableMap] with standard implementations")
            .withFeatures(
                CollectionSize.ANY,
                MapFeature.REJECTS_DUPLICATES_AT_CREATION,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                CollectionFeature.KNOWN_ORDER)
            .createTestSuite());

    return suite;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testForwarding() {
    new ForwardingWrapperTester()
        .testForwarding(
            Map.class,
            new Function<Map, Map>() {
              @Override
              public Map apply(Map delegate) {
                return wrap(delegate);
              }
            });
  }

  public void testEquals() {
    Map<Integer, String> map1 = ImmutableMap.of(1, "one");
    Map<Integer, String> map2 = ImmutableMap.of(2, "two");
    new EqualsTester()
        .addEqualityGroup(map1, wrap(map1), wrap(map1))
        .addEqualityGroup(map2, wrap(map2))
        .testEquals();
  }

  public void testStandardEntrySet() throws InvocationTargetException {
    @SuppressWarnings("unchecked")
    final Map<String, Boolean> map = mock(Map.class);

    Map<String, Boolean> forward =
        new ForwardingMap<String, Boolean>() {
          @Override
          protected Map<String, Boolean> delegate() {
            return map;
          }

          @Override
          public Set<Entry<String, Boolean>> entrySet() {
            return new StandardEntrySet() {
              @Override
              public Iterator<Entry<String, Boolean>> iterator() {
                return Iterators.emptyIterator();
              }
            };
          }
        };
    callAllPublicMethods(new TypeToken<Set<Entry<String, Boolean>>>() {}, forward.entrySet());

    // These are the methods specified by StandardEntrySet
    verify(map, atLeast(0)).clear();
    verify(map, atLeast(0)).containsKey(any());
    verify(map, atLeast(0)).get(any());
    verify(map, atLeast(0)).isEmpty();
    verify(map, atLeast(0)).remove(any());
    verify(map, atLeast(0)).size();
    verifyNoMoreInteractions(map);
  }

  public void testStandardKeySet() throws InvocationTargetException {
    @SuppressWarnings("unchecked")
    final Map<String, Boolean> map = mock(Map.class);

    Map<String, Boolean> forward =
        new ForwardingMap<String, Boolean>() {
          @Override
          protected Map<String, Boolean> delegate() {
            return map;
          }

          @Override
          public Set<String> keySet() {
            return new StandardKeySet();
          }
        };
    callAllPublicMethods(new TypeToken<Set<String>>() {}, forward.keySet());

    // These are the methods specified by StandardKeySet
    verify(map, atLeast(0)).clear();
    verify(map, atLeast(0)).containsKey(any());
    verify(map, atLeast(0)).isEmpty();
    verify(map, atLeast(0)).remove(any());
    verify(map, atLeast(0)).size();
    verify(map, atLeast(0)).entrySet();
    verifyNoMoreInteractions(map);
  }

  public void testStandardValues() throws InvocationTargetException {
    @SuppressWarnings("unchecked")
    final Map<String, Boolean> map = mock(Map.class);

    Map<String, Boolean> forward =
        new ForwardingMap<String, Boolean>() {
          @Override
          protected Map<String, Boolean> delegate() {
            return map;
          }

          @Override
          public Collection<Boolean> values() {
            return new StandardValues();
          }
        };
    callAllPublicMethods(new TypeToken<Collection<Boolean>>() {}, forward.values());

    // These are the methods specified by StandardValues
    verify(map, atLeast(0)).clear();
    verify(map, atLeast(0)).containsValue(any());
    verify(map, atLeast(0)).isEmpty();
    verify(map, atLeast(0)).size();
    verify(map, atLeast(0)).entrySet();
    verifyNoMoreInteractions(map);
  }

  public void testToStringWithNullKeys() throws Exception {
    Map<String, String> hashmap = Maps.newHashMap();
    hashmap.put("foo", "bar");
    hashmap.put(null, "baz");

    StandardImplForwardingMap<String, String> forwardingMap =
        new StandardImplForwardingMap<>(Maps.<String, String>newHashMap());
    forwardingMap.put("foo", "bar");
    forwardingMap.put(null, "baz");

    assertEquals(hashmap.toString(), forwardingMap.toString());
  }

  public void testToStringWithNullValues() throws Exception {
    Map<String, String> hashmap = Maps.newHashMap();
    hashmap.put("foo", "bar");
    hashmap.put("baz", null);

    StandardImplForwardingMap<String, String> forwardingMap =
        new StandardImplForwardingMap<>(Maps.<String, String>newHashMap());
    forwardingMap.put("foo", "bar");
    forwardingMap.put("baz", null);

    assertEquals(hashmap.toString(), forwardingMap.toString());
  }

  private static <K, V> Map<K, V> wrap(final Map<K, V> delegate) {
    return new ForwardingMap<K, V>() {
      @Override
      protected Map<K, V> delegate() {
        return delegate;
      }
    };
  }

  private static final ImmutableMap<String, String> JUF_METHODS =
      ImmutableMap.of(
          "java.util.function.Predicate", "test",
          "java.util.function.Consumer", "accept",
          "java.util.function.IntFunction", "apply");

  private static Object getDefaultValue(final TypeToken<?> type) {
    Class<?> rawType = type.getRawType();
    Object defaultValue = ArbitraryInstances.get(rawType);
    if (defaultValue != null) {
      return defaultValue;
    }

    final String typeName = rawType.getCanonicalName();
    if (JUF_METHODS.containsKey(typeName)) {
      // Generally, methods that accept java.util.function.* instances
      // don't like to get null values.  We generate them dynamically
      // using Proxy so that we can have Java 7 compliant code.
      return Reflection.newProxy(
          rawType,
          new AbstractInvocationHandler() {
            @Override
            public Object handleInvocation(Object proxy, Method method, Object[] args) {
              // Crude, but acceptable until we can use Java 8.  Other
              // methods have default implementations, and it is hard to
              // distinguish.
              if (method.getName().equals(JUF_METHODS.get(typeName))) {
                return getDefaultValue(type.method(method).getReturnType());
              }
              throw new IllegalStateException("Unexpected " + method + " invoked on " + proxy);
            }
          });
    } else {
      return null;
    }
  }

  private static <T> void callAllPublicMethods(TypeToken<T> type, T object)
      throws InvocationTargetException {
    for (Method method : type.getRawType().getMethods()) {
      if ((method.getModifiers() & STATIC) != 0) {
        continue;
      }
      ImmutableList<Parameter> parameters = type.method(method).getParameters();
      Object[] args = new Object[parameters.size()];
      for (int i = 0; i < parameters.size(); i++) {
        args[i] = getDefaultValue(parameters.get(i).getType());
      }
      try {
        try {
          method.invoke(object, args);
        } catch (InvocationTargetException ex) {
          try {
            throw ex.getCause();
          } catch (UnsupportedOperationException unsupported) {
            // this is a legit exception
          }
        }
      } catch (Throwable cause) {
        throw new InvocationTargetException(cause, method + " with args: " + Arrays.toString(args));
      }
    }
  }
}
