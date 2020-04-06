/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public final static DataFormat REPORT = new DataFormat(SystemEntityType.REPORT.name());
    public final static DataFormat ACTIVITY = new DataFormat(SystemEntityType.ACTIVITY.name());
    
    public static DataFormat getByType(SystemEntityType t) {
        switch(t) {
            case ACTIVITY: return ACTIVITY;
            case CONTAINER: return CONTAINER;
            case EVENT: return EVENT;
            case REPORT: return REPORT;
            case PARAMETER: return PARAMETER;
            default: throw new IllegalArgumentException("System entity type " + t + " unknown");
        }
    }
}
