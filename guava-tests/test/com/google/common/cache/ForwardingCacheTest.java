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

import junit.framework.TestCase;

import java.util.concurrent.ExecutionException;

/**
 * Unit test for {@link ForwardingCache}.
 *
 * @author Charles Fry
 */
public class ForwardingCacheTest extends TestCase {
  private Cache<String, Boolean> forward;
  private Cache<String, Boolean> mock;

  @SuppressWarnings("unchecked") // createMock
  @Override public void setUp() throws Exception {
    super.setUp();
    /*
     * Class parameters must be raw, so we can't create a proxy with generic
     * type arguments. The created proxy only records calls and returns null, so
     * the type is irrelevant at runtime.
     */
    mock = createMock(Cache.class);
    forward = new ForwardingCache<String, Boolean>() {
      @Override protected Cache<String, Boolean> delegate() {
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

  public void testInvalidateAll() {
    mock.invalidateAll();
    replay(mock);
    forward.invalidateAll();
    verify(mock);
  }

  public void testSize() {
    expect(mock.size()).andReturn(0);
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

  public void testActiveEntries() {
    expect(mock.activeEntries(10)).andReturn(null);
    replay(mock);
    assertNull(forward.activeEntries(10));
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
}
