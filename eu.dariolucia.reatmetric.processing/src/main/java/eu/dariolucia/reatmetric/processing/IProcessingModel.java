/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.ISystemModelProvisionService;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
import eu.dariolucia.reatmetric.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;

import java.util.List;

public interface IProcessingModel extends ISystemModelProvisionService {

    void injectParameters(List<ParameterSample> sampleList);

    void raiseEvent(EventOccurrence event, List<ParameterSample> attachedParameters);

    IUniqueId startAction(); // TODO

    void reportActionProgress(); // TODO

    void enable(SystemEntityPath path, boolean recursive);

    void disable(SystemEntityPath path, boolean recursive);

    ProcessingDefinition getProcessingDefinition();

}
