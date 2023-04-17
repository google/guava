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

package com.google.common.testing;

import com.google.common.annotations.GwtCompatible;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests may use this to intercept messages that are logged by the code under test. Example:
 *
 * <pre>
 *   TestLogHandler handler;
 *
 *   protected void setUp() throws Exception {
 *     super.setUp();
 *     handler = new TestLogHandler();
 *     SomeClass.logger.addHandler(handler);
 *     addTearDown(new TearDown() {
 *       public void tearDown() throws Exception {
 *         SomeClass.logger.removeHandler(handler);
 *       }
 *     });
 *   }
 *
 *   public void test() {
 *     SomeClass.foo();
 *     LogRecord firstRecord = handler.getStoredLogRecords().get(0);
 *     assertEquals("some message", firstRecord.getMessage());
 *   }
 * </pre>
 *
 * @author Kevin Bourrillion
 * @since 10.0
 */
@GwtCompatible
public class TestLogHandler extends Handler {
  /** We will keep a private list of all logged records */
  private final List<LogRecord> list = new ArrayList<>();

  /** Adds the most recently logged record to our list. */
  @Override
  public synchronized void publish(@Nullable LogRecord record) {
    list.add(record);
  }

  @Override
  public void flush() {}

  @Override
  public void close() {}

  public synchronized void clear() {
    list.clear();
  }

  /** Returns a snapshot of the logged records. */
  /*
   * TODO(cpovirk): consider higher-level APIs here (say, assertNoRecordsLogged(),
   * getOnlyRecordLogged(), getAndClearLogRecords()...)
   *
   * TODO(cpovirk): consider renaming this method to reflect that it takes a snapshot (and/or return
   * an ImmutableList)
   */
  public synchronized List<LogRecord> getStoredLogRecords() {
    List<LogRecord> result = new ArrayList<>(list);
    return Collections.unmodifiableList(result);
  }
}
