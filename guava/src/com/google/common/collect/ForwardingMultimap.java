/*
 * Copyright (C) 2007 The Guava Authors
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

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.AnnotatedFor;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A multimap which forwards all its method calls to another multimap.
 * Subclasses should override one or more methods to modify the behavior of
 * the backing multimap as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * @author Robert Konigsberg
 * @since 2.0
 */
@AnnotatedFor({"nullness"})
@GwtCompatible
public abstract class ForwardingMultimap<K extends /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object, V extends /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object> extends ForwardingObject implements Multimap<K, V> {

  /** Constructor for use by subclasses. */
  protected ForwardingMultimap() {}

  @Override
  protected abstract Multimap<K, V> delegate();

  @Override
  public Map<K, Collection<V>> asMap() {
    return delegate().asMap();
  }

  @Override
  public void clear() {
    delegate().clear();
  }

  @Pure
  @Override
  public boolean containsEntry(/*@Nullable*/ /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object key, /*@Nullable*/ /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object value) {
    return delegate().containsEntry(key, value);
  }

  @Pure
  @Override
  public boolean containsKey(/*@Nullable*/ /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object key) {
    return delegate().containsKey(key);
  }

  @Pure
  @Override
  public boolean containsValue(/*@Nullable*/ /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object value) {
    return delegate().containsValue(value);
  }

  @SideEffectFree
  @Override
  public Collection<Entry<K, V>> entries() {
    return delegate().entries();
  }

  @Override
  public Collection<V> get(/*@Nullable*/ /*@org.checkerframework.checker.nullness.qual.Nullable*/ K key) {
    return delegate().get(key);
  }

  @Pure
  @Override
  public boolean isEmpty() {
    return delegate().isEmpty();
  }

  @Override
  public Multiset<K> keys() {
    return delegate().keys();
  }

  @SideEffectFree
  @Override
  public Set<K> keySet() {
    return delegate().keySet();
  }

  @Override
  public boolean put(K key, V value) {
    return delegate().put(key, value);
  }

  @Override
  public boolean putAll(K key, Iterable<? extends V> values) {
    return delegate().putAll(key, values);
  }

  @Override
  public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
    return delegate().putAll(multimap);
  }

  @Override
  public boolean remove(/*@Nullable*/ /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object key, /*@Nullable*/ /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object value) {
    return delegate().remove(key, value);
  }

  @Override
  public Collection<V> removeAll(/*@Nullable*/ /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object key) {
    return delegate().removeAll(key);
  }

  @Override
  public Collection<V> replaceValues(K key, Iterable<? extends V> values) {
    return delegate().replaceValues(key, values);
  }

  @Pure
  @Override
  public int size() {
    return delegate().size();
  }

  @SideEffectFree
  @Override
  public Collection<V> values() {
    return delegate().values();
  }

  @Pure
  @Override
  public boolean equals(/*@Nullable*/ /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object object) {
    return object == this || delegate().equals(object);
  }

  @Pure
  @Override
  public int hashCode() {
    return delegate().hashCode();
  }
}
