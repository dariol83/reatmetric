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
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
import eu.dariolucia.reatmetric.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;

import java.util.List;

public interface IProcessingModel {

    void injectParameters(List<ParameterSample> sampleList);

    void raiseEvent(EventOccurrence event);

    IUniqueId startActivity(); // TODO

    void reportActivityProgress(); // TODO

    ProcessingDefinition getProcessingDefinition();

    void enable(SystemEntityPath path) throws ProcessingModelException;

    void disable(SystemEntityPath path) throws ProcessingModelException;

    SystemEntity getRoot() throws ProcessingModelException;

    List<SystemEntity> getContainedEntities(SystemEntityPath se) throws ProcessingModelException;

    SystemEntity getSystemEntityAt(SystemEntityPath path) throws ProcessingModelException;

    SystemEntity getSystemEntityOf(int id) throws ProcessingModelException;

    int getExternalIdOf(SystemEntityPath path) throws ProcessingModelException;

    SystemEntityPath getPathOf(int id) throws ProcessingModelException;
}
