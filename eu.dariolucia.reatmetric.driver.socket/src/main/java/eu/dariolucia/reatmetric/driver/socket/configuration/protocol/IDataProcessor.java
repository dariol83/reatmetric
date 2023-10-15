/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.socket.configuration.protocol;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.rawdata.RawData;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface IDataProcessor {

    String getHandlerName();

    void forwardRawData(RawData data);

    void forwardParameters(List<ParameterSample> samples);

    void forwardEvents(List<EventOccurrence> events);

    void forwardActivityProgress(ActivityProgress progressReport);

    IUniqueId getNextRawDataId();

    Timer getTimerService();

    <V> Future<V> execute(Callable<V> task);

    Future<?> execute(Runnable task);
}
