/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.common;

/**
 *
 * @author dario
 */
public enum FieldType {
    BOOLEAN, // map to Java boolean
    STRING, // map to Java java.lang.String
    REAL, // map to Java double
    INTEGER, // map to Java long
    ABSOLUTE_TIME, // map to Java java.time.Instant
    ENUM, // map to Java java.lang.Enumeration
    HIDDEN // map to Java java.lang.Object, no info available, not shown
}
