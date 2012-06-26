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

package java.util.concurrent;

import java.util.Map;

/**
 * Minimal GWT emulation of a map providing atomic operations.
 *
 * @author Jesse Wilson
 */
public interface ConcurrentMap<K, V> extends Map<K, V> {

  V putIfAbsent(K key, V value);

  boolean remove(Object key, Object value);

  V replace(K key, V value);

  boolean replace(K key, V oldValue, V newValue);
}
