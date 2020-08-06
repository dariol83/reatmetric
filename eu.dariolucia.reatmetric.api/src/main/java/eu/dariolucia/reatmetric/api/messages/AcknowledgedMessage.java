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

package eu.dariolucia.reatmetric.api.messages;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;

import java.io.Serializable;
import java.time.Instant;

public class AcknowledgedMessage extends AbstractDataItem implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final OperationalMessage message;

    private final AcknowledgementState state;

    private final Instant acknowledgementTime;

    private final String user;

    public AcknowledgedMessage(IUniqueId internalId, Instant generationTime, OperationalMessage message, AcknowledgementState state, Instant acknowledgementTime, String user, Object extension) {
        super(internalId, generationTime, extension);
        this.message = message;
        this.state = state;
        this.acknowledgementTime = acknowledgementTime;
        this.user = user;
    }

    public OperationalMessage getMessage() {
        return message;
    }

    public AcknowledgementState getState() {
        return state;
    }

    public Instant getAcknowledgementTime() {
        return acknowledgementTime;
    }

    public String getUser() {
        return user;
    }

    public AcknowledgedMessage ack(String user) {
        return new AcknowledgedMessage(getInternalId(), getGenerationTime(), getMessage(), AcknowledgementState.ACKNOWLEDGED, Instant.now(), user, getExtension());
    }

    @Override
    public String toString() {
        return "AcknowledgedMessage{" +
                "message=" + message +
                ", state=" + state +
                ", acknowledgementTime=" + acknowledgementTime +
                ", user='" + user + '\'' +
                "}";
    }
}
