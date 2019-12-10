/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

public class ValidityException extends Exception {

    public ValidityException(String message) {
        super(message);
    }

    public ValidityException(String message, Throwable cause) {
        super(message, cause);
    }
}
