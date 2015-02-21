package com.google.common.util.concurrent;

import java.util.concurrent.*;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ExecutorBuilderTest {
    @Test
    public void testBuildThreadPoolExecutor() throws Exception {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) ExecutorBuilder.newBuilder().corePoolSize(10).build("MyThread");
        assertThat(executor.getCorePoolSize(), is(10));
        Future<String> future = executor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return Thread.currentThread().getName();
            }
        });
        assertTrue(future.get().contains("MyThread-"));
        assertThat(executor.getKeepAliveTime(TimeUnit.SECONDS), is(0L));
    }

    @Test
    public void test_simple_config() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) ExecutorBuilder.newBuilder().build();
        assertThat(executor.getCorePoolSize(), is(Runtime.getRuntime().availableProcessors()));
        assertThat(executor.getMaximumPoolSize(), is(Runtime.getRuntime().availableProcessors()));
        assertThat(executor.getKeepAliveTime(TimeUnit.SECONDS), is(0L));
        assertTrue(executor.getQueue() instanceof LinkedBlockingQueue);
        assertTrue(executor.getRejectedExecutionHandler() instanceof ThreadPoolExecutor.AbortPolicy);
    }

    @Test
    public void test_max_size() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) ExecutorBuilder.newBuilder().maxPoolSize(10).build();
        assertThat(executor.getMaximumPoolSize(), is(10));
    }

    @Test
    public void test_keep_alive() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) ExecutorBuilder.newBuilder().keepAliveSeconds(10).build();
        assertThat(executor.getKeepAliveTime(TimeUnit.SECONDS), is(10L));
    }

    @Test
    public void test_work_queue() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) ExecutorBuilder.newBuilder().workQueue(new ArrayBlockingQueue<Runnable>(100)).build();
        assertTrue(executor.getQueue() instanceof ArrayBlockingQueue);
    }

    @Test
    public void test_reject_exec_handler() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) ExecutorBuilder.newBuilder()
                .rejectHandler(new ThreadPoolExecutor.DiscardPolicy()).build();
        assertTrue(executor.getRejectedExecutionHandler() instanceof ThreadPoolExecutor.DiscardPolicy);
    }

    @Test
    public void test_simple_scheduled() {
        ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor) ExecutorBuilder.newBuilder().buildScheduled();
        assertThat(scheduler.getCorePoolSize(), is(Runtime.getRuntime().availableProcessors()));
    }

    @Test
    public void testBuildScheduleThreadPoolExecutor() throws Exception {
        ScheduledExecutorService scheduler = ExecutorBuilder.newBuilder().corePoolSize(10).buildScheduled("MyThread");
        assertThat(((ThreadPoolExecutor) scheduler).getCorePoolSize(), is(10));
        Future<String> future = scheduler.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return Thread.currentThread().getName();
            }
        });
        assertTrue(future.get().contains("MyThread-"));
        assertTrue(scheduler instanceof ScheduledThreadPoolExecutor);
    }
}