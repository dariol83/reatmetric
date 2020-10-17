/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package eu.dariolucia.reatmetric.ui.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * This object is responsible to collect all the updates arriving from a backend service (e.g. alarms, parameters)
 * and forward them to the rendering function in batches.
 * If the updates are really a lot, there is the risk that the rendering function thread is killed by the amount of rendering
 * requests. This object implements a very simple sistem that allows to defer the rendering request if there are too many
 * requests packing together the updates. As optional feature that can be enabled, it can DROP the oldest updates, so that only
 * the specified (maximum) number of updates is forwarded for rendering.
 */
public final class DataProcessingDelegator<T> {

	private static final int RENDERING_LATENCY_TIME = 100; // In milliseconds

	// Temporary object queue
	private final BlockingDeque<T> temporaryQueue = new LinkedBlockingDeque<>();

	private final Lock temporaryQueueLock = new ReentrantLock();
	private final Condition temporaryQueueCondition = this.temporaryQueueLock.newCondition();

	// Delegator thread, set daemon so that it is killed if the application exits
	private final ExecutorService delegator;

	private final Consumer<List<T>> actionee;

	private final Integer maxItemsToForward;

	private long lastForwardingTime = System.currentTimeMillis();

	private boolean suspended = false;

	public DataProcessingDelegator(Consumer<List<T>> actionee) {
		this(null, actionee, null);
	}

	public DataProcessingDelegator(String name, Consumer<List<T>> actionee) {
		this(name, actionee, null);
	}

	public DataProcessingDelegator(String name, Consumer<List<T>> actionee, Integer maxItemsToForward) {
		this.maxItemsToForward = maxItemsToForward;
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
				// Until the delegation is suspended OR the queue is empty and the delegation is not shut down, wait
				while (this.suspended || (this.temporaryQueue.isEmpty() && !this.delegator.isShutdown())) {
					this.temporaryQueueCondition.await();
				}
				// At this stage, the delegator is not suspended, but it could be shut down, so check
				if (this.delegator.isShutdown()) {
					return;
				}
				// At this stage you have something in the queue: before draining it and send, let's see when was the last time forwarded it
				long currentUpdate = System.currentTimeMillis();
				boolean keepWaiting = true;
				// Max 10 updates per second?
				while(currentUpdate - this.lastForwardingTime < RENDERING_LATENCY_TIME && keepWaiting) {
					// We wait
					keepWaiting = this.temporaryQueueCondition.await(RENDERING_LATENCY_TIME, TimeUnit.MILLISECONDS);
					currentUpdate = System.currentTimeMillis();
				}
				// Now we forward the contents of the queue ot the rendered
				List<T> tempList = new LinkedList<>();
				this.temporaryQueue.drainTo(tempList);
				if(maxItemsToForward != null && maxItemsToForward < tempList.size()) {
					tempList = tempList.subList(tempList.size() - maxItemsToForward, tempList.size());
				}
				// Remember when you are forwarding it
				this.lastForwardingTime = currentUpdate;
				// Send the whole queue to the rendering function
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
