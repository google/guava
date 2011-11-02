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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import java.util.concurrent.ExecutionException;

/**
 * Unit test for {@link ForwardingLoadingCache}.
 *
 * @author Charles Fry
 */
public class ForwardingLoadingCacheTest extends TestCase {
  private LoadingCache<String, Boolean> forward;
  private LoadingCache<String, Boolean> mock;

  @SuppressWarnings("unchecked") // createMock
  @Override public void setUp() throws Exception {
    super.setUp();
    /*
     * Class parameters must be raw, so we can't create a proxy with generic
     * type arguments. The created proxy only records calls and returns null, so
     * the type is irrelevant at runtime.
     */
    mock = createMock(LoadingCache.class);
    forward = new ForwardingLoadingCache<String, Boolean>() {
      @Override protected LoadingCache<String, Boolean> delegate() {
        return mock;
      }
    };
  }

  public void testGet() throws ExecutionException {
    expect(mock.get("key")).andReturn(Boolean.TRUE);
    replay(mock);
    assertSame(Boolean.TRUE, forward.get("key"));
    verify(mock);
  }

  public void testGetUnchecked() {
    expect(mock.getUnchecked("key")).andReturn(Boolean.TRUE);
    replay(mock);
    assertSame(Boolean.TRUE, forward.getUnchecked("key"));
    verify(mock);
  }

  public void testGetAll() throws ExecutionException {
    expect(mock.getAll(ImmutableList.of("key"))).andReturn(ImmutableMap.of("key", Boolean.TRUE));
    replay(mock);
    assertEquals(ImmutableMap.of("key", Boolean.TRUE), forward.getAll(ImmutableList.of("key")));
    verify(mock);
  }

  public void testApply() {
    expect(mock.apply("key")).andReturn(Boolean.TRUE);
    replay(mock);
    assertSame(Boolean.TRUE, forward.apply("key"));
    verify(mock);
  }

  public void testInvalidate() {
    mock.invalidate("key");
    replay(mock);
    forward.invalidate("key");
    verify(mock);
  }

  public void testRefresh() throws ExecutionException {
    mock.refresh("key");
    replay(mock);
    forward.refresh("key");
    verify(mock);
  }

  public void testInvalidateAll() {
    mock.invalidateAll();
    replay(mock);
    forward.invalidateAll();
    verify(mock);
  }

  public void testSize() {
    expect(mock.size()).andReturn(0L);
    replay(mock);
    forward.size();
    verify(mock);
  }

  public void testStats() {
    expect(mock.stats()).andReturn(null);
    replay(mock);
    assertNull(forward.stats());
    verify(mock);
  }

  public void testAsMap() {
    expect(mock.asMap()).andReturn(null);
    replay(mock);
    assertNull(forward.asMap());
    verify(mock);
  }

  public void testCleanUp() {
    mock.cleanUp();
    replay(mock);
    forward.cleanUp();
    verify(mock);
  }

  /**
   * Make sure that all methods are forwarded.
   */
  private static class OnlyGet<K, V> extends ForwardingLoadingCache<K, V> {
    @Override
    protected LoadingCache<K, V> delegate() {
      return null;
    }
  }
}
