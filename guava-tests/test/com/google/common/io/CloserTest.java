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

package com.google.common.io;

import static com.google.common.base.Throwables.throwIfInstanceOf;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@link Closer}.
 *
 * @author Colin Decker
 */
@NullUnmarked
public class CloserTest extends TestCase {

  private TestSuppressor suppressor;

  @Override
  protected void setUp() throws Exception {
    suppressor = new TestSuppressor();
  }

  public void testNoExceptionsThrown() throws IOException {
    Closer closer = new Closer(suppressor);

    TestCloseable c1 = closer.register(TestCloseable.normal());
    TestCloseable c2 = closer.register(TestCloseable.normal());
    TestCloseable c3 = closer.register(TestCloseable.normal());

    assertFalse(c1.isClosed());
    assertFalse(c2.isClosed());
    assertFalse(c3.isClosed());

    closer.close();

    assertTrue(c1.isClosed());
    assertTrue(c2.isClosed());
    assertTrue(c3.isClosed());

    assertTrue(suppressor.suppressions.isEmpty());
  }

  public void testExceptionThrown_fromTryBlock() throws IOException {
    Closer closer = new Closer(suppressor);

    TestCloseable c1 = closer.register(TestCloseable.normal());
    TestCloseable c2 = closer.register(TestCloseable.normal());

    IOException exception = new IOException();

    try {
      try {
        throw exception;
      } catch (Throwable e) {
        throw closer.rethrow(e);
      } finally {
        closer.close();
      }
    } catch (Throwable expected) {
      assertSame(exception, expected);
    }

    assertTrue(c1.isClosed());
    assertTrue(c2.isClosed());

    assertTrue(suppressor.suppressions.isEmpty());
  }

  public void testExceptionThrown_whenCreatingCloseables() throws IOException {
    Closer closer = new Closer(suppressor);

    TestCloseable c1 = null;
    TestCloseable c2 = null;
    TestCloseable c3 = null;
    try {
      try {
        c1 = closer.register(TestCloseable.normal());
        c2 = closer.register(TestCloseable.normal());
        c3 = closer.register(TestCloseable.throwsOnCreate());
      } catch (Throwable e) {
        throw closer.rethrow(e);
      } finally {
        closer.close();
      }
    } catch (Throwable expected) {
      assertThat(expected).isInstanceOf(IOException.class);
    }

    assertTrue(c1.isClosed());
    assertTrue(c2.isClosed());
    assertNull(c3);

    assertTrue(suppressor.suppressions.isEmpty());
  }

  public void testExceptionThrown_whileClosingLastCloseable() throws IOException {
    Closer closer = new Closer(suppressor);

    IOException exception = new IOException();

    // c1 is added first, closed last
    TestCloseable c1 = closer.register(TestCloseable.throwsOnClose(exception));
    TestCloseable c2 = closer.register(TestCloseable.normal());

    try {
      closer.close();
    } catch (Throwable expected) {
      assertSame(exception, expected);
    }

    assertTrue(c1.isClosed());
    assertTrue(c2.isClosed());

    assertTrue(suppressor.suppressions.isEmpty());
  }

  public void testExceptionThrown_whileClosingFirstCloseable() throws IOException {
    Closer closer = new Closer(suppressor);

    IOException exception = new IOException();

    // c2 is added last, closed first
    TestCloseable c1 = closer.register(TestCloseable.normal());
    TestCloseable c2 = closer.register(TestCloseable.throwsOnClose(exception));

    try {
      closer.close();
    } catch (Throwable expected) {
      assertSame(exception, expected);
    }

    assertTrue(c1.isClosed());
    assertTrue(c2.isClosed());

    assertTrue(suppressor.suppressions.isEmpty());
  }

  public void testCloseExceptionsSuppressed_whenExceptionThrownFromTryBlock() throws IOException {
    Closer closer = new Closer(suppressor);

    IOException tryException = new IOException();
    IOException c1Exception = new IOException();
    IOException c2Exception = new IOException();

    TestCloseable c1 = closer.register(TestCloseable.throwsOnClose(c1Exception));
    TestCloseable c2 = closer.register(TestCloseable.throwsOnClose(c2Exception));

    try {
      try {
        throw tryException;
      } catch (Throwable e) {
        throw closer.rethrow(e);
      } finally {
        closer.close();
      }
    } catch (Throwable expected) {
      assertSame(tryException, expected);
    }

    assertTrue(c1.isClosed());
    assertTrue(c2.isClosed());

    assertSuppressed(
        new Suppression(c2, tryException, c2Exception),
        new Suppression(c1, tryException, c1Exception));
  }

  public void testCloseExceptionsSuppressed_whenExceptionThrownClosingFirstCloseable()
      throws IOException {
    Closer closer = new Closer(suppressor);

    IOException c1Exception = new IOException();
    IOException c2Exception = new IOException();
    IOException c3Exception = new IOException();

    TestCloseable c1 = closer.register(TestCloseable.throwsOnClose(c1Exception));
    TestCloseable c2 = closer.register(TestCloseable.throwsOnClose(c2Exception));
    TestCloseable c3 = closer.register(TestCloseable.throwsOnClose(c3Exception));

    try {
      closer.close();
    } catch (Throwable expected) {
      assertSame(c3Exception, expected);
    }

    assertTrue(c1.isClosed());
    assertTrue(c2.isClosed());
    assertTrue(c3.isClosed());

    assertSuppressed(
        new Suppression(c2, c3Exception, c2Exception),
        new Suppression(c1, c3Exception, c1Exception));
  }

  public void testRuntimeExceptions() throws IOException {
    Closer closer = new Closer(suppressor);

    RuntimeException tryException = new RuntimeException();
    RuntimeException c1Exception = new RuntimeException();
    RuntimeException c2Exception = new RuntimeException();

    TestCloseable c1 = closer.register(TestCloseable.throwsOnClose(c1Exception));
    TestCloseable c2 = closer.register(TestCloseable.throwsOnClose(c2Exception));

    try {
      try {
        throw tryException;
      } catch (Throwable e) {
        throw closer.rethrow(e);
      } finally {
        closer.close();
      }
    } catch (Throwable expected) {
      assertSame(tryException, expected);
    }

    assertTrue(c1.isClosed());
    assertTrue(c2.isClosed());

    assertSuppressed(
        new Suppression(c2, tryException, c2Exception),
        new Suppression(c1, tryException, c1Exception));
  }

  public void testErrors() throws IOException {
    Closer closer = new Closer(suppressor);

    Error c1Exception = new Error();
    Error c2Exception = new Error();
    Error c3Exception = new Error();

    TestCloseable c1 = closer.register(TestCloseable.throwsOnClose(c1Exception));
    TestCloseable c2 = closer.register(TestCloseable.throwsOnClose(c2Exception));
    TestCloseable c3 = closer.register(TestCloseable.throwsOnClose(c3Exception));

    try {
      closer.close();
    } catch (Throwable expected) {
      assertSame(c3Exception, expected);
    }

    assertTrue(c1.isClosed());
    assertTrue(c2.isClosed());
    assertTrue(c3.isClosed());

    assertSuppressed(
        new Suppression(c2, c3Exception, c2Exception),
        new Suppression(c1, c3Exception, c1Exception));
  }

  public static void testSuppressingSuppressor() throws IOException {
    Closer closer = Closer.create();

    IOException thrownException = new IOException();
    IOException c1Exception = new IOException();
    RuntimeException c2Exception = new RuntimeException();

    TestCloseable c1 = closer.register(TestCloseable.throwsOnClose(c1Exception));
    TestCloseable c2 = closer.register(TestCloseable.throwsOnClose(c2Exception));
    try {
      try {
        throw thrownException;
      } catch (Throwable e) {
        throw closer.rethrow(thrownException, IOException.class);
      } finally {
        assertThat(thrownException.getSuppressed()).isEmpty();
        closer.close();
      }
    } catch (IOException expected) {
      assertSame(thrownException, expected);
    }

    assertTrue(c1.isClosed());
    assertTrue(c2.isClosed());

    ImmutableSet<Throwable> suppressed = ImmutableSet.copyOf(thrownException.getSuppressed());
    assertEquals(2, suppressed.size());

    assertEquals(ImmutableSet.of(c1Exception, c2Exception), suppressed);
  }

  public void testNullCloseable() throws IOException {
    Closer closer = Closer.create();
    closer.register(null);
    closer.close();
  }

  /**
   * Asserts that an exception was thrown when trying to close each of the given throwables and that
   * each such exception was suppressed because of the given thrown exception.
   */
  private void assertSuppressed(Suppression... expected) {
    assertEquals(ImmutableList.copyOf(expected), suppressor.suppressions);
  }

  // TODO(cpovirk): Just use addSuppressed+getSuppressed now that we can rely on it.
  /** Suppressor that records suppressions. */
  private static class TestSuppressor implements Closer.Suppressor {

    private final List<Suppression> suppressions = new ArrayList<>();

    @Override
    public void suppress(Closeable closeable, Throwable thrown, Throwable suppressed) {
      suppressions.add(new Suppression(closeable, thrown, suppressed));
    }
  }

  /** Record of a call to suppress. */
  private static class Suppression {
    private final Closeable closeable;
    private final Throwable thrown;
    private final Throwable suppressed;

    private Suppression(Closeable closeable, Throwable thrown, Throwable suppressed) {
      this.closeable = closeable;
      this.thrown = thrown;
      this.suppressed = suppressed;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof Suppression) {
        Suppression other = (Suppression) obj;
        return closeable.equals(other.closeable)
            && thrown.equals(other.thrown)
            && suppressed.equals(other.suppressed);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(closeable, thrown, suppressed);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("closeable", closeable)
          .add("thrown", thrown)
          .add("suppressed", suppressed)
          .toString();
    }
  }

  private static class TestCloseable implements Closeable {

    private final Throwable throwOnClose;
    private boolean closed;

    static TestCloseable normal() throws IOException {
      return new TestCloseable(null);
    }

    static TestCloseable throwsOnClose(Throwable throwOnClose) throws IOException {
      return new TestCloseable(throwOnClose);
    }

    static TestCloseable throwsOnCreate() throws IOException {
      throw new IOException();
    }

    private TestCloseable(@Nullable Throwable throwOnClose) {
      this.throwOnClose = throwOnClose;
    }

    boolean isClosed() {
      return closed;
    }

    @Override
    public void close() throws IOException {
      closed = true;
      if (throwOnClose != null) {
        throwIfInstanceOf(throwOnClose, IOException.class);
        throwIfUnchecked(throwOnClose);
        throw new AssertionError(throwOnClose);
      }
    }
  }
}
