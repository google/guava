package com.google.common.util.concurrent;

import java.util.AbstractList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;


public class BlockingQueueWithBufferTest extends TestCase{
    public void testCreateWithNegativeSize() throws Exception {
        try {
            BlockingQueueWithBuffer.create(-1);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testCreateWithZeroSize() throws Exception {
        try {
            BlockingQueueWithBuffer.create(0);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testMoreCapacity_maxSize1() {
        BlockingQueueWithBuffer<String> queue = BlockingQueueWithBuffer.create(1);
        assertEquals(1, queue.remainingCapacity());
        queue.add("hi");
        assertEquals(0, queue.remainingCapacity());
    }

    public void testMoreCapacity_maxSize() {
        BlockingQueueWithBuffer<String> queue = BlockingQueueWithBuffer.create(3);
        assertEquals(3, queue.remainingCapacity());
        queue.add("hi");
        assertEquals(2, queue.remainingCapacity());
        queue.add("hi");
        assertEquals(1, queue.remainingCapacity());
        queue.add("hi");
        assertEquals(0, queue.remainingCapacity());
    }

    public void testAdditionAfterOne() throws Exception {
        EvictingBlockingQueue<String> queue = EvictingBlockingQueue.create(1);
        assertEquals(0, queue.size());
        assertEquals(1, queue.remainingCapacity());

        assertTrue(queue.add("hi"));
        assertEquals("hi", queue.element());
        assertEquals("hi", queue.peek());
        assertEquals(1, queue.size());
        assertEquals(0, queue.remainingCapacity());

        assertTrue(queue.add("there"));
        assertEquals("there", queue.element());
        assertEquals("there", queue.peek());
        assertEquals(1, queue.size());
        assertEquals(0, queue.remainingCapacity());

        assertEquals("there", queue.remove());
        assertEquals(0, queue.size());
        assertEquals(1, queue.remainingCapacity());
    }

    public void testAdditionAfterThree() throws Exception {
        BlockingQueueWithBuffer<String> queue = BlockingQueueWithBuffer.create(3);
        assertEquals(0, queue.size());
        assertEquals(3, queue.remainingCapacity());

        assertTrue(queue.add("one"));
        assertTrue(queue.offer("two",Long.valueOf(1),TimeUnit.SECONDS));
        assertTrue(queue.offer("three"));
        assertEquals("one", queue.element());
        assertEquals("one", queue.peek());
        assertEquals(3, queue.size());
        assertEquals(0, queue.remainingCapacity());

        assertTrue(queue.add("four"));
        assertEquals("two", queue.element());
        assertEquals("two", queue.peek());
        assertEquals(3, queue.size());
        assertEquals(0, queue.remainingCapacity());

        assertEquals("two", queue.remove());
        assertEquals(2, queue.size());
        assertEquals(1, queue.remainingCapacity());
    }

    public void testAddAll() throws Exception {
        BlockingQueueWithBuffer<String> queue = BlockingQueueWithBuffer.create(3);
        assertEquals(0, queue.size());
        assertEquals(3, queue.remainingCapacity());

        assertTrue(queue.addAll(ImmutableList.of("one", "two", "three")));
        assertEquals("one", queue.element());
        assertEquals("one", queue.peek());
        assertEquals(3, queue.size());
        assertEquals(0, queue.remainingCapacity());

        assertTrue(queue.addAll(ImmutableList.of("four")));
        assertEquals("two", queue.element());
        assertEquals("two", queue.peek());
        assertEquals(3, queue.size());
        assertEquals(0, queue.remainingCapacity());

        assertEquals("two", queue.remove());
        assertEquals(2, queue.size());
        assertEquals(1, queue.remainingCapacity());
    }

    public void testAddAll_largeList() {
        final List<String> list = ImmutableList.of("one", "two", "three", "four", "five");
        List<String> misbehavingList = new AbstractList<String>() {
            @Override
            public int size() {
                return list.size();
            }

            @Override
            public String get(int index) {
                if (index < 2) {
                    throw new AssertionError();
                }
                return list.get(index);
            }
        };

        BlockingQueueWithBuffer<String> queue = BlockingQueueWithBuffer.create(3);
        assertTrue(queue.addAll(misbehavingList));
        assertEquals("three", queue.remove());
        assertEquals("four", queue.remove());
        assertEquals("five", queue.remove());
        assertTrue(queue.isEmpty());
    }

    @GwtIncompatible // NullPointerTester
    public void testNullPointerExceptions() {
        NullPointerTester tester = new NullPointerTester();
        tester.testAllPublicStaticMethods(BlockingQueueWithBuffer.class);
        tester.testAllPublicConstructors(BlockingQueueWithBuffer.class);
        BlockingQueueWithBuffer<String> queue = BlockingQueueWithBuffer.create(5);
        // The queue must be non-empty so it throws a NPE correctly
        queue.add("one");
        tester.testAllPublicInstanceMethods(queue);
    }

    public void testSerialization() {
       BlockingQueueWithBuffer<String> original = BlockingQueueWithBuffer.create(5);
        original.add("one");
        original.add("two");
        original.add("three");

        BlockingQueueWithBuffer<String> copy = SerializableTester.reserialize(original);
        assertEquals(copy.maxSize, original.maxSize);
        assertEquals("one", copy.remove());
        assertEquals("two", copy.remove());
        assertEquals("three", copy.remove());
        assertTrue(copy.isEmpty());
    }
}
