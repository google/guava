package com.google.common.util.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ExecutorBuilder {
    private int corePoolSize;
    private int maxPoolSize;
    private long keepAlive;
    private BlockingQueue<Runnable> workQueue;
    private InternalThreadFactory threadFactory;
    private RejectedExecutionHandler handler;

    public static ExecutorBuilder newBuilder() {
        return new ExecutorBuilder();
    }

    public ExecutorBuilder corePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        return this;
    }

    public ExecutorBuilder maxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
        return this;
    }

    public ExecutorBuilder keepAliveSeconds(int keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public ExecutorBuilder workQueue(BlockingQueue<Runnable> queue) {
        this.workQueue = queue;
        return this;
    }

    public ExecutorBuilder rejectHandler(RejectedExecutionHandler handler) {
        this.handler = handler;
        return this;
    }

    private void initThreadFactoryIfNecessary() {
        if (threadFactory == null) {
            threadFactory = new InternalThreadFactory();
        }
    }

    public ThreadPoolExecutor buildExecutor(String name, Supplier<ThreadPoolExecutor> threadPoolExecutorSupplier) {
        if (name != null) {
            initThreadFactoryIfNecessary();
            threadFactory.setThreadNamePrefix(name);
        }

        configurePoolSize();

        if (workQueue == null) {
            workQueue = new LinkedBlockingQueue<Runnable>();
        }

        ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorSupplier.get();

        if (threadFactory != null)
            threadPoolExecutor.setThreadFactory(threadFactory);

        if (keepAlive >= 0) {
            threadPoolExecutor.setKeepAliveTime(keepAlive, TimeUnit.SECONDS);
        }

        if (handler != null) {
            threadPoolExecutor.setRejectedExecutionHandler(handler);
        }

        return threadPoolExecutor;
    }

    private void configurePoolSize() {
        if (corePoolSize == 0)
            corePoolSize = Runtime.getRuntime().availableProcessors();

        maxPoolSize = maxPoolSize == 0 ? corePoolSize : maxPoolSize;
    }

    public ExecutorService build() {
        return build(null);
    }

    public ExecutorService build(String name) {
        return buildExecutor(name, new Supplier<ThreadPoolExecutor>() {
            @Override
            public ThreadPoolExecutor get() {
                return new ThreadPoolExecutor(corePoolSize, maxPoolSize, 0, TimeUnit.SECONDS, workQueue);
            }
        });
    }

    public ScheduledExecutorService buildScheduled() {
        return buildScheduled(null);
    }

    public ScheduledExecutorService buildScheduled(String name) {
        return (ScheduledExecutorService) buildExecutor(name, new Supplier<ThreadPoolExecutor>() {
            @Override
            public ThreadPoolExecutor get() {
                return new ScheduledThreadPoolExecutor(corePoolSize);
            }
        });
    }

    private class InternalThreadFactory implements ThreadFactory {
        private ThreadFactory baseThreadFactory = Executors.defaultThreadFactory();
        private AtomicInteger threadCount = new AtomicInteger(1);

        private String threadNamePrefix;

        public InternalThreadFactory() {
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = baseThreadFactory.newThread(r);
            if (threadNamePrefix != null)
                thread.setName(threadNamePrefix + "-" + threadCount.getAndIncrement());
            return thread;
        }

        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }
    }
}