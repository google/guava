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

package com.google.common.cache;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.ExecutionException;
import junit.framework.TestCase;

/**
 * Unit test for {@link ForwardingCache}.
 *
 * @author Charles Fry
 */
public class ForwardingCacheTest extends TestCase {
  private Cache<String, Boolean> forward;
  private Cache<String, Boolean> mock;

  @SuppressWarnings({"unchecked", "DoNotMock"}) // mock
  @Override
  public void setUp() throws Exception {
    super.setUp();
    /*
     * Class parameters must be raw, so we can't create a proxy with generic
     * type arguments. The created proxy only records calls and returns null, so
     * the type is irrelevant at runtime.
     */
    mock = mock(Cache.class);
    forward =
        new ForwardingCache<String, Boolean>() {
          @Override
          protected Cache<String, Boolean> delegate() {
            return mock;
          }
        };
  }

  public void testGetIfPresent() throws ExecutionException {
    when(mock.getIfPresent("key")).thenReturn(Boolean.TRUE);
    assertSame(Boolean.TRUE, forward.getIfPresent("key"));
  }

  public void testGetAllPresent() throws ExecutionException {
    when(mock.getAllPresent(ImmutableList.of("key")))
        .thenReturn(ImmutableMap.of("key", Boolean.TRUE));
    assertEquals(
        ImmutableMap.of("key", Boolean.TRUE), forward.getAllPresent(ImmutableList.of("key")));
  }

  public void testInvalidate() {
    forward.invalidate("key");
    verify(mock).invalidate("key");
  }

  public void testInvalidateAllIterable() {
    forward.invalidateAll(ImmutableList.of("key"));
    verify(mock).invalidateAll(ImmutableList.of("key"));
  }

  public void testInvalidateAll() {
    forward.invalidateAll();
    verify(mock).invalidateAll();
  }

  public void testSize() {
    when(mock.size()).thenReturn(0L);
    assertEquals(0, forward.size());
  }

  public void testStats() {
    when(mock.stats()).thenReturn(null);
    assertNull(forward.stats());
  }

  public void testAsMap() {
    when(mock.asMap()).thenReturn(null);
    assertNull(forward.asMap());
  }

  public void testCleanUp() {
    forward.cleanUp();
    verify(mock).cleanUp();
  }

  /** Make sure that all methods are forwarded. */
  private static class OnlyGet<K, V> extends ForwardingCache<K, V> {
    @Override
    protected Cache<K, V> delegate() {
      return null;
    }
  }
}
