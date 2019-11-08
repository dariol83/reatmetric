/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */
package eu.dariolucia.reatmetric.api.common.exceptions;

/**
 *
 * @author dario
 */
public class MonitoringCentreException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = -3949315104317147362L;

	public MonitoringCentreException() {
    }

    public MonitoringCentreException(String message) {
        super(message);
    }

    public MonitoringCentreException(String message, Throwable cause) {
        super(message, cause);
    }

    public MonitoringCentreException(Throwable cause) {
        super(cause);
    }
    
}
