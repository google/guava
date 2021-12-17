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

import com.google.common.base.Function;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.ForwardingWrapperTester;
import junit.framework.TestCase;

/**
 * Tests {@link ForwardingTable}.
 *
 * @author Gregory Kick
 */
public class ForwardingTableTest extends TestCase {

  @SuppressWarnings("rawtypes")
  public void testForwarding() {
    new ForwardingWrapperTester()
        .testForwarding(
            Table.class,
            new Function<Table, Table>() {
              @Override
              public Table apply(Table delegate) {
                return wrap(delegate);
              }
            });
  }

  public void testEquals() {
    Table<Integer, Integer, String> table1 = ImmutableTable.of(1, 1, "one");
    Table<Integer, Integer, String> table2 = ImmutableTable.of(2, 2, "two");
    new EqualsTester()
        .addEqualityGroup(table1, wrap(table1), wrap(table1))
        .addEqualityGroup(table2, wrap(table2))
        .testEquals();
  }

  private static <R, C, V> Table<R, C, V> wrap(final Table<R, C, V> delegate) {
    return new ForwardingTable<R, C, V>() {
      @Override
      protected Table<R, C, V> delegate() {
        return delegate;
      }
    };
  }
}
