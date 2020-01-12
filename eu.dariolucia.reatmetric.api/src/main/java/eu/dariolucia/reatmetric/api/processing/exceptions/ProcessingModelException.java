/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.processing.exceptions;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

public class ProcessingModelException extends ReatmetricException {

    public ProcessingModelException(String s) {
        super(s);
    }

    public ProcessingModelException(Throwable cause) {
        super(cause);
    }

    public ProcessingModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
