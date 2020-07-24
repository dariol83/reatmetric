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

package eu.dariolucia.reatmetric.api.processing;

import java.time.Instant;

/**
 * This class contains a snapshot of the performance of the processing model. The sampling period depends on the specific
 * implementation of the processing model.
 *
 * This class is immutable.
 */
public final class ProcessingModelStats {

    private final Instant time;

    private final int pendingInjections;

    private final double dataItemsPerSecond;

    public ProcessingModelStats(Instant time, int pendingInjections, double dataItemsPerSecond) {
        this.time = time;
        this.pendingInjections = pendingInjections;
        this.dataItemsPerSecond = dataItemsPerSecond;
    }

    /**
     * The generation time of the snapshot.
     *
     * @return the generation time of the snapshot
     */
    public Instant getTime() {
        return time;
    }

    /**
     * The number of injections requests (parameters and events) pending inside the telemetry queue.
     * This is not the number of parameter samples and event reports pending processing, but the number of bulk requests.
     *
     * @return the number of injections pending inside the telemetry queue.
     */
    public int getPendingInjections() {
        return pendingInjections;
    }

    /**
     * The number of {@link eu.dariolucia.reatmetric.api.common.AbstractDataItem} generated per second (on average).
     *
     * @return the {@link eu.dariolucia.reatmetric.api.common.AbstractDataItem} production rate
     */
    public double getDataItemsPerSecond() {
        return dataItemsPerSecond;
    }

    @Override
    public String toString() {
        return "time=" + time +
                ", pending injections " + pendingInjections +
                ", data items/second " + dataItemsPerSecond;
    }
}
