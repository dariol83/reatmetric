/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing;

public class ActivityHandlingException extends Exception {

    public ActivityHandlingException(String message) {
        super(message);
    }

    public ActivityHandlingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActivityHandlingException(Throwable cause) {
        super(cause);
    }
}
