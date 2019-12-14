/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.extension.internal;

import eu.dariolucia.reatmetric.processing.definition.scripting.IBindingResolver;
import eu.dariolucia.reatmetric.processing.extension.ICheckExtension;

import java.time.Instant;
import java.util.Map;

public class NoCheck implements ICheckExtension {
    @Override
    public String getFunctionName() {
        return "__nocheck";
    }

    @Override
    public boolean check(Object currentValue, Instant generationTime, Map<String, String> properties, IBindingResolver resolver) {
        return false;
    }
}
