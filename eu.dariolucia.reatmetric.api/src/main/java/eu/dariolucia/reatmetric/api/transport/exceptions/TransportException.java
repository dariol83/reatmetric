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

package eu.dariolucia.reatmetric.api.transport.exceptions;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

/**
 * Exception thrown when an error occurs in a transport connector.
 */
public class TransportException extends ReatmetricException {

    /**
     * Constructs a new TransportException with the specified message.
     *
     * @param message the detail message
     */
    public TransportException(String message) {
        super(message);
    }

    /**
     * Constructs a new TransportException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public TransportException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new TransportException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public TransportException(Throwable cause) {
        super(cause);
    }
}
