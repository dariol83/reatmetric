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
package eu.dariolucia.reatmetric.api.common.exceptions;

/**
 * Base-class for ReatMetric-defined exceptions.
 */
public class ReatmetricException extends Exception {

    /**
     * The serial version UID for serialization compatibility.
     */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new ReatMetric exception with null as its detail message.
	 */
	public ReatmetricException() {
    }

	/**
	 * Constructs a new ReatMetric exception with the specified detail message.
	 *
	 * @param message the detail message (can be null)
	 */
    public ReatmetricException(String message) {
        super(message);
    }

	/**
	 * Constructs a new ReatMetric exception with the specified detail message and cause.
	 *
	 * @param message the detail message (can be null)
	 * @param cause the cause (can be null)
	 */
    public ReatmetricException(String message, Throwable cause) {
        super(message, cause);
    }

	/**
	 * Constructs a new ReatMetric exception with the specified cause and a detail message of
	 * (cause==null ? null : cause.toString()).
	 *
	 * @param cause the cause (can be null)
	 */
    public ReatmetricException(Throwable cause) {
        super(cause);
    }
    
}
