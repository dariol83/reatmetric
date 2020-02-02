/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

public class ValueReferenceException extends ReatmetricException {

    public ValueReferenceException(String message) {
        super(message);
    }

    public ValueReferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
