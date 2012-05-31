/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.reflect;

import com.google.common.annotations.Beta;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * A mutable type-to-instance map.
 * See also {@link ImmutableTypeToInstanceMap}.
 *
 * @author Ben Yu
 * @since 13.0
 */
@Beta
public final class MutableTypeToInstanceMap<B> extends ForwardingMap<TypeToken<? extends B>, B>
    implements TypeToInstanceMap<B> {

  private final Map<TypeToken<? extends B>, B> backingMap = Maps.newHashMap();

  @Nullable
  @Override
  public <T extends B> T getInstance(Class<T> type) {
    return trustedGet(TypeToken.of(type));
  }

  @Nullable
  @Override
  public <T extends B> T putInstance(Class<T> type, @Nullable T value) {
    return trustedPut(TypeToken.of(type), value);
  }

  @Nullable
  @Override
  public <T extends B> T getInstance(TypeToken<T> type) {
    return trustedGet(type.rejectTypeVariables());
  }

  @Nullable
  @Override
  public <T extends B> T putInstance(TypeToken<T> type, @Nullable T value) {
    return trustedPut(type.rejectTypeVariables(), value);
  }

  /** Not supported. Use {@link #putInstance} instead. */
  @Override public B put(TypeToken<? extends B> key, B value) {
    throw new UnsupportedOperationException("Please use putInstance() instead.");
  }

  /** Not supported. Use {@link #putInstance} instead. */
  @Override public void putAll(Map<? extends TypeToken<? extends B>, ? extends B> map) {
    throw new UnsupportedOperationException("Please use putInstance() instead.");
  }

  @Override protected Map<TypeToken<? extends B>, B> delegate() {
    return backingMap;
  }

  @SuppressWarnings("unchecked") // value could not get in if not a T
  @Nullable
  private <T extends B> T trustedPut(TypeToken<T> type, @Nullable T value) {
    return (T) backingMap.put(type, value);
  }

  @SuppressWarnings("unchecked") // value could not get in if not a T
  @Nullable
  private <T extends B> T trustedGet(TypeToken<T> type) {
    return (T) backingMap.get(type);
  }
}
