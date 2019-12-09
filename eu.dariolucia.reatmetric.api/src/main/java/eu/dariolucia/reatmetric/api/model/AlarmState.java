/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.model;

/**
 *
 * @author dario
 */
public enum AlarmState {
    ALARM(true),
    WARNING(true),
    VIOLATED(false),
    ERROR(true),
    NOMINAL(false),
    NOT_CHECKED(false),
    NOT_APPLICABLE(false),
    UNKNOWN(false);

    private final boolean alarm;

    AlarmState(boolean alarm) {
        this.alarm = alarm;
    }

    public boolean isAlarm() {
        return alarm;
    }
}
