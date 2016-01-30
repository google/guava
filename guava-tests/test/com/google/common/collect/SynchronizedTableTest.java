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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Synchronized.SynchronizedCollection;
import com.google.common.collect.Synchronized.SynchronizedMap;
import com.google.common.collect.Synchronized.SynchronizedSet;
import com.google.common.collect.Table.Cell;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;

/**
 * Tests for {@code Synchronized#table}.
 *
 * @author  Sean P. Floyd
 */
public class SynchronizedTableTest extends TestCase {

    private static final Object ROW_KEY = "foo";
    private static final Object COLUMN_KEY = "bar";
    private static final Object VALUE = "baz";

    Object mutex = new Integer(1); // something Serializable

    protected <R, C, V> Table<R, C, V> create() {
        TestTable<R, C, V> inner = new TestTable<R, C, V>(HashBasedTable.<R, C, V>create(), mutex);
        Table<R, C, V> outer = Synchronized.table(inner, mutex);
        return outer;
    }

    static class TestTable<R, C, V> extends ForwardingTable<R, C, V> implements Serializable {
        public Object mutex;
        private Table<R, C, V> delegate;

        public TestTable(Table<R, C, V> delegate, Object mutex) {
            checkNotNull(mutex);
            this.delegate = delegate;
            this.mutex = mutex;
        }

        @Override
        protected Table<R, C, V> delegate() {
            return delegate;
        }

        @Override
        public Set<Cell<R, C, V>> cellSet() {
            assertTrue(Thread.holdsLock(mutex));
            return super.cellSet();
        }

        @Override
        public void clear() {
            assertTrue(Thread.holdsLock(mutex));
            super.clear();
        }

        @Override
        public Map<R, V> column(C columnKey) {
            assertTrue(Thread.holdsLock(mutex));
            return super.column(columnKey);
        }

        @Override
        public Set<C> columnKeySet() {
            assertTrue(Thread.holdsLock(mutex));
            return super.columnKeySet();
        }

        @Override
        public Map<C, Map<R, V>> columnMap() {
            assertTrue(Thread.holdsLock(mutex));
            return super.columnMap();
        }

        @Override
        public boolean contains(Object rowKey, Object columnKey) {
            assertTrue(Thread.holdsLock(mutex));
            return super.contains(rowKey, columnKey);
        }

        @Override
        public boolean containsColumn(Object columnKey) {
            assertTrue(Thread.holdsLock(mutex));
            return super.containsColumn(columnKey);
        }

        @Override
        public boolean containsRow(Object rowKey) {
            assertTrue(Thread.holdsLock(mutex));
            return super.containsRow(rowKey);
        }

        @Override
        public boolean containsValue(Object value) {
            assertTrue(Thread.holdsLock(mutex));
            return super.containsValue(value);
        }

        @Override
        public V get(Object rowKey, Object columnKey) {
            assertTrue(Thread.holdsLock(mutex));
            return super.get(rowKey, columnKey);
        }

        @Override
        public boolean isEmpty() {
            assertTrue(Thread.holdsLock(mutex));
            return super.isEmpty();
        }

        @Override
        public V put(R rowKey, C columnKey, V value) {
            assertTrue(Thread.holdsLock(mutex));
            return super.put(rowKey, columnKey, value);
        }

        @Override
        public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
            assertTrue(Thread.holdsLock(mutex));
            super.putAll(table);
        }

        @Override
        public V remove(Object rowKey, Object columnKey) {
            assertTrue(Thread.holdsLock(mutex));
            return super.remove(rowKey, columnKey);
        }

        @Override
        public Map<C, V> row(R rowKey) {
            assertTrue(Thread.holdsLock(mutex));
            return super.row(rowKey);
        }

        @Override
        public Set<R> rowKeySet() {
            assertTrue(Thread.holdsLock(mutex));
            return super.rowKeySet();
        }

        @Override
        public Map<R, Map<C, V>> rowMap() {
            assertTrue(Thread.holdsLock(mutex));
            return super.rowMap();
        }

        @Override
        public int size() {
            assertTrue(Thread.holdsLock(mutex));
            return super.size();
        }

        @Override
        public Collection<V> values() {
            assertTrue(Thread.holdsLock(mutex));
            return super.values();
        }

        @Override
        public boolean equals(Object obj) {
            assertTrue(Thread.holdsLock(mutex));
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            assertTrue(Thread.holdsLock(mutex));
            return super.hashCode();
        }

        private static long serialVersionUID = 0;
    }

  /*
   * Copied from SynchronizedMapTest:
   * This is somewhat of a weak test; we verify that all of the methods are
   * correct, but not that they're actually forwarding correctly. We also rely
   * on the other tests (e.g., SynchronizedSetTest) to verify that the
   * collection views are synchronized correctly.
   */

    public void testSize() {
        create().size();
    }

    public void testIsEmpty() {
        create().isEmpty();
    }

    public void testRemove() {
        create().remove(null, null);
    }

    public void testClear() {
        create().clear();
    }

    public void testContains() {
        create().contains(null, null);
    }

    public void testContainsRow() {
        create().containsRow(null);
    }

    public void testContainsColumn() {
        create().containsColumn(null);
    }

    public void testGet() {
        create().get(null, null);
    }

    public void testPut() {
        create().put(ROW_KEY, COLUMN_KEY, VALUE);
    }

    public void testPutAll() {
        create().putAll(HashBasedTable.create());
    }

    public void testValues() {
        Collection<Object> values = create().values();
        assertTrue(values instanceof SynchronizedCollection);
        assertSame(mutex, ((SynchronizedCollection<?>) values).mutex);
    }

    public void testContainsValue() {
        create().containsValue(null);
    }

    public void testRow() {
        Map<Object, Object> row = create().row(ROW_KEY);
        assertTrue(row instanceof SynchronizedMap);
        assertSame(mutex, ((SynchronizedMap<?, ?>) row).mutex);
    }

    public void testColumn() {
        Map<Object, Object> column = create().column(COLUMN_KEY);
        assertTrue(column instanceof SynchronizedMap);
        assertSame(mutex, ((SynchronizedMap<?, ?>) column).mutex);
    }

    public void testCellSet() {
        Set<Cell<Object, Object, Object>> cellSet = create().cellSet();
        assertTrue(cellSet instanceof SynchronizedSet);
        assertSame(mutex, ((SynchronizedSet<?>) cellSet).mutex);
    }

    public void testRowKeySet() {
        Set<Object> rowKeySet = create().rowKeySet();
        assertTrue(rowKeySet instanceof SynchronizedSet);
        assertSame(mutex, ((SynchronizedSet<?>) rowKeySet).mutex);
    }

    public void testColumnKeySet() {
        Set<Object> columnKeySet = create().columnKeySet();
        assertTrue(columnKeySet instanceof SynchronizedSet);
        assertSame(mutex, ((SynchronizedSet<?>) columnKeySet).mutex);
    }

    public void testRowMap() {
        Table<Object, Object, Object> table = create();
        table.put(ROW_KEY, COLUMN_KEY, VALUE);

        Map<Object, Map<Object, Object>> rowMap = table.rowMap();
        assertTrue(rowMap instanceof SynchronizedMap);
        assertSame(mutex, ((SynchronizedMap<?, ?>) rowMap).mutex);

        Map<Object, Object> row = rowMap.get(ROW_KEY);
        assertTrue(row instanceof SynchronizedMap);
        assertSame(mutex, ((SynchronizedMap<?, ?>) row).mutex);

    }

    public void testColumnMap() {
        Table<Object, Object, Object> table = create();
        table.put(ROW_KEY, COLUMN_KEY, VALUE);

        Map<Object, Map<Object, Object>> columnMap = table.columnMap();
        assertTrue(columnMap instanceof SynchronizedMap);
        assertSame(mutex, ((SynchronizedMap<?, ?>) columnMap).mutex);

        Map<Object, Object> column = columnMap.get(COLUMN_KEY);
        assertTrue(column instanceof SynchronizedMap);
        assertSame(mutex, ((SynchronizedMap<?, ?>) column).mutex);
    }

    public void testEquals() {
        create().equals(HashBasedTable.create());
    }

    public void testHashCode() {
        create().hashCode();
    }

    public void testToString() {
        create().toString();
    }

    public void testSerialization() {
        SerializableTester.reserializeAndAssert(create());
    }

}
