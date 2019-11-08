/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.extension;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.processing.impl.IParameterResolver;

/**
 * Use of the {@link eu.dariolucia.ccsds.encdec.extension.ExtensionId} annotation on implementations to indicate
 * the ID of the extension. This is the identifier that must be put inside the database.
 */
public interface ICheckExtension {

    AlarmState check(Object currentValue, int currentViolations, IParameterResolver resolver);
}
