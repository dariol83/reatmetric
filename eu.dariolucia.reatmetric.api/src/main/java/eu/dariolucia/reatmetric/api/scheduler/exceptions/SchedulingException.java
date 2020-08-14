package eu.dariolucia.reatmetric.api.scheduler.exceptions;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

public class SchedulingException extends ReatmetricException {

    public SchedulingException() {
    }

    public SchedulingException(String message) {
        super(message);
    }

    public SchedulingException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchedulingException(Throwable cause) {
        super(cause);
    }
}
