/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.extension.internal;

import eu.dariolucia.reatmetric.processing.definition.scripting.IBindingResolver;
import eu.dariolucia.reatmetric.processing.extension.ICalibrationExtension;

import java.util.Map;

public class IdentityCalibration implements ICalibrationExtension {

    @Override
    public String getFunctionName() {
        return "__identity";
    }

    @Override
    public Object calibrate(Object valueToCalibrate, Map<String, String> properties, IBindingResolver resolver) {
        return valueToCalibrate;
    }
}
