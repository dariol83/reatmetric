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

package eu.dariolucia.reatmetric.driver.spacecraft.activity;

import eu.dariolucia.ccsds.tmtc.util.AnnotatedObject;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcPacketInfo;

import java.io.Serializable;

public abstract class AbstractTcTracker implements Serializable {

    private final IActivityHandler.ActivityInvocation invocation;
    private final TcPacketInfo info;
    private final RawData rawData;

    protected AbstractTcTracker(IActivityHandler.ActivityInvocation invocation, TcPacketInfo info, RawData rd) {
        this.invocation = invocation;
        this.info = info;
        this.rawData = rd;
    }

    public RawData getRawData() {
        return rawData;
    }

    public IActivityHandler.ActivityInvocation getInvocation() {
        return invocation;
    }

    public TcPacketInfo getInfo() {
        return info;
    }

    public abstract AnnotatedObject getObject();

}
