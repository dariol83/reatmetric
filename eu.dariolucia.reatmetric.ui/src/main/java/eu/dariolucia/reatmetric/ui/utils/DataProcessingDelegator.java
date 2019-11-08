/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 *
 * @author dario
 */
public final class DataProcessingDelegator<T> {

	// Temporary object queue
	private final BlockingQueue<T> temporaryQueue = new LinkedBlockingQueue<>();

	private final Lock temporaryQueueLock = new ReentrantLock();
	private final Condition temporaryQueueCondition = this.temporaryQueueLock.newCondition();

	// Delegator thread, set daemon so that it is killed if the application exits
	private final ExecutorService delegator;

	private final Consumer<List<T>> actionee;

	private boolean suspended = false;

	public DataProcessingDelegator(Consumer<List<T>> actionee) {
		this(null, actionee);
	}

	public DataProcessingDelegator(String name, Consumer<List<T>> actionee) {
		this.actionee = actionee;
		this.delegator = Executors.newFixedThreadPool(1, (r) -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName(name != null ? name : "Delegator Thread: " + actionee);
			return t;
		});
		this.delegator.execute(this::delegatorRun);
	}

	private void delegatorRun() {
		while (!this.delegator.isShutdown()) {
			this.temporaryQueueLock.lock();
			try {
				while (this.suspended || (this.temporaryQueue.isEmpty() && !this.delegator.isShutdown())) {
					this.temporaryQueueCondition.await();
				}
				if (this.delegator.isShutdown()) {
					return;
				}
				List<T> tempList = new LinkedList<>();
				this.temporaryQueue.drainTo(tempList);

				this.actionee.accept(tempList);
			} catch (InterruptedException e) {
				Thread.interrupted();
			} finally {
				this.temporaryQueueLock.unlock();
			}
		}
	}

	public void delegate(T object) {
		this.temporaryQueueLock.lock();
		try {
			if (object != null) {
				this.temporaryQueue.add(object);
				this.temporaryQueueCondition.signalAll();
			}
		} finally {
			this.temporaryQueueLock.unlock();
		}
	}

	public void delegate(List<T> objects) {
		this.temporaryQueueLock.lock();
		try {
			if (objects != null && !objects.isEmpty()) {
				this.temporaryQueue.addAll(objects);
				this.temporaryQueueCondition.signalAll();
			}
		} finally {
			this.temporaryQueueLock.unlock();
		}
	}

	public void shutdown() {
		this.delegator.shutdown();
		this.temporaryQueueLock.lock();
		try {
			this.temporaryQueueCondition.signalAll();
		} finally {
			this.temporaryQueueLock.unlock();
		}
		try {
			this.delegator.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void suspend() {
		this.temporaryQueueLock.lock();
		try {
			this.suspended = true;
			this.temporaryQueue.clear();
			this.temporaryQueueCondition.signalAll();
		} finally {
			this.temporaryQueueLock.unlock();
		}
	}

	public void resume() {
		this.temporaryQueueLock.lock();
		try {
			this.suspended = false;
			this.temporaryQueueCondition.signalAll();
		} finally {
			this.temporaryQueueLock.unlock();
		}
	}
}
