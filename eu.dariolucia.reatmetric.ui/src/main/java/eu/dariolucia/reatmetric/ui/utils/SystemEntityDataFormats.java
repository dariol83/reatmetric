/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.utils;

import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import javafx.scene.input.DataFormat;

/**
 *
 * @author dario
 */
public class SystemEntityDataFormats {
    public final static DataFormat CONTAINER = new DataFormat(SystemEntityType.CONTAINER.name());
    public final static DataFormat PARAMETER = new DataFormat(SystemEntityType.PARAMETER.name());
    public final static DataFormat EVENT = new DataFormat(SystemEntityType.EVENT.name());
    public final static DataFormat REFERENCE = new DataFormat(SystemEntityType.REFERENCE.name());
    public final static DataFormat REPORT = new DataFormat(SystemEntityType.REPORT.name());
    public final static DataFormat ACTIVITY = new DataFormat(SystemEntityType.ACTIVITY.name());
    
    public static DataFormat getByType(SystemEntityType t) {
        switch(t) {
            case ACTIVITY: return ACTIVITY;
            case CONTAINER: return CONTAINER;
            case EVENT: return EVENT;
            case REFERENCE: return REFERENCE;
            case REPORT: return REPORT;
            case PARAMETER: return PARAMETER;
            default: throw new IllegalArgumentException("System entity type " + t + " unknown");
        }
    }
}
