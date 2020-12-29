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

package eu.dariolucia.reatmetric.ui.udd;

import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;

import java.util.List;

public interface IChartDisplayController {

    void dispose();

    void systemDisconnected(IReatmetricSystem system);

    void systemConnected(IReatmetricSystem system);

    EventDataFilter getCurrentEventFilter();

    ParameterDataFilter getCurrentParameterFilter();

    boolean isLive();

    void updateDataItems(List<? extends AbstractDataItem> value);
}
