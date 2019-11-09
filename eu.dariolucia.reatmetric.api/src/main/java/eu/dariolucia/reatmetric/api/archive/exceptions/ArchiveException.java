/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.archive.exceptions;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

public class ArchiveException extends ReatmetricException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ArchiveException() {
    }

    public ArchiveException(String message) {
        super(message);
    }

    public ArchiveException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArchiveException(Throwable cause) {
        super(cause);
    }
}
