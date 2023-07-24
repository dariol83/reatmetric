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

/**
 * This class represents an {@link OperationalMessage} with an acknowledgement status.
 *
 * Objects of this class are immutable.
 */
public class AcknowledgedMessage extends AbstractDataItem implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final OperationalMessage message;

    private final AcknowledgementState state;

    private final Instant acknowledgementTime;

    private final String user;

    /**
     * Create a new instance of the object.
     *
     * @param internalId the internal ID of this object
     * @param generationTime the generation time of the acknowledged message
     * @param message the {@link OperationalMessage} linked to this acknowledged message
     * @param state the acknowledgement status (PENDING or ACKNOWLEDGED)
     * @param acknowledgementTime the acknowledgement time: can be null if the acknowledgement status is PENDING
     * @param user the user who acknowledged the message: can be null if the acknowledgement status is PENDING
     * @param extension the extension object
     */
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

    /**
     * Calling this method creates an 'acknowledged' version of the object.
     *
     * @param user the user who acknowledged the message
     * @return the new {@link AcknowledgedMessage}
     */
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
