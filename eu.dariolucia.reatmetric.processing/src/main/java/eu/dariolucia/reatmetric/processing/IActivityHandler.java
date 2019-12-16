/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.SystemEntity;

import java.util.List;
import java.util.Map;

public interface IActivityHandler {

    void registerModel(IProcessingModel model);

    List<String> getSupportedRoutes();

    List<String> getSupportedActivityTypes();

    void executeActivity(IUniqueId activityOccurrenceId, SystemEntity activityEntity, Map<String, Object> arguments, Map<String, String> properties, String route) throws ActivityHandlingException;
}
