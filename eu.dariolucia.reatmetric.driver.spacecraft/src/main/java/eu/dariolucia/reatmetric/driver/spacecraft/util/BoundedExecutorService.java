/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.spacecraft.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BoundedExecutorService implements ExecutorService {

    private static final Logger LOG = Logger.getLogger(BoundedExecutorService.class.getName());

    private static final int DEFAULT_MAX_QUEUE_SIZE = 1000;
    private static final ThreadFactory DEFAULT_THREAD_FACTORY = r -> {
        Thread t = new Thread(r);
        t.setName("Bounded Executor Service Thread");
        t.setDaemon(true);
        return t;
    };

    private final List<Thread> threads;
    private final BlockingQueue<FutureTask<?>> tasks;
    private final AtomicInteger terminationBarrier = new AtomicInteger(0);
    private final Lock objectLock = new ReentrantLock();
    private final Condition terminationCondition = objectLock.newCondition();
    private volatile Consumer<Throwable> exceptionHandler;

    private volatile boolean shutdown = false;

    public BoundedExecutorService(int numThreads) {
        this(numThreads, DEFAULT_MAX_QUEUE_SIZE);
    }

    public BoundedExecutorService(int numThreads, int maxQueueSize) {
        this(numThreads, maxQueueSize, DEFAULT_THREAD_FACTORY);
    }

    public BoundedExecutorService(int numThreads, int maxQueueSize, ThreadFactory threadFactory) {
        this.threads = new ArrayList<>(numThreads);
        this.tasks = new ArrayBlockingQueue<>(maxQueueSize);
        this.terminationBarrier.set(numThreads);
        for(int i = 0; i < numThreads; ++i) {
            Thread t = threadFactory.newThread(new ProcessorRunnable());
            this.threads.add(t);
            t.start();
        }
        // Default exception handler - Log
        exceptionHandler = o -> LOG.log(Level.WARNING, "", o);
    }

    public void setExceptionHandler(Consumer<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void shutdown() {
        this.objectLock.lock();
        try {
            shutdown = true;
            for(Thread t : threads) {
                t.interrupt();
            }
            this.terminationCondition.signalAll();
        } finally {
            this.objectLock.unlock();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        this.objectLock.lock();
        try {
            shutdown();
            List<Runnable> toReturn = new LinkedList<>();
            tasks.drainTo(toReturn);
            return toReturn;
        } finally {
            this.objectLock.unlock();
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return shutdown && terminationBarrier.get() == 0;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        this.objectLock.lock();
        try {
            while(!shutdown || terminationBarrier.get() != 0) {
                boolean result = terminationCondition.await(timeout, unit); // Wrong implementation, I know, but good enough for now
                if(!result) {
                    return false; // Timeout expired
                }
            }
            return true;
        } finally {
            this.objectLock.unlock();
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        checkForShutdown();
        return addTask(new FutureTask<>(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        checkForShutdown();
        FutureTask<T> future = new FutureTask<>(task, result);
        return addTask(future);
    }

    @Override
    public Future<?> submit(Runnable task) {
        checkForShutdown();
        FutureTask<?> future = new FutureTask<>(task, null);
        return addTask(future);
    }

    @Override
    public void execute(Runnable command) {
        checkForShutdown();
        FutureTask<?> future = new FutureTask<>(command, null);
        addTask(future);
    }

    private void checkForShutdown() {
        if (isShutdown()) {
            throw new IllegalStateException("Task refused - Executor is shut down");
        }
    }

    private <T> FutureTask<T> addTask(FutureTask<T> future) {
        try {
            tasks.put(future);
        } catch (InterruptedException e) {
            throw new RuntimeException("Task refused - Thread interrupted");
        }
        return future;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("Not supported");
    }

    private class ProcessorRunnable implements Runnable {
        @Override
        public void run() {
            while(!shutdown) {
                try {
                    FutureTask<?> taskToRun = tasks.take();
                    try {
                        taskToRun.run();
                    } catch (Throwable t) {
                        Consumer<Throwable> exHdl = exceptionHandler;
                        if (exHdl != null) {
                            exHdl.accept(t);
                        }
                    }
                } catch (InterruptedException e) {
                    // Ignore and continue
                }
            }
            objectLock.lock();
            try {
                terminationBarrier.decrementAndGet();
                terminationCondition.signalAll();
            } finally {
                objectLock.unlock();
            }
        }
    }
}
