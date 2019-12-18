/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.processors;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.definition.ActivityProcessingDefinition;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.input.AbstractInputDataItem;
import eu.dariolucia.reatmetric.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.processing.input.ActivityRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityProcessor extends AbstractSystemEntityProcessor<ActivityProcessingDefinition, ActivityOccurrenceData, ActivityProgress> {

    private final Map<IUniqueId, ActivityOccurrenceProcessor> id2occurrence = new HashMap<>();

    public ActivityProcessor(ActivityProcessingDefinition act, ProcessingModelImpl processingModel) {
        super(act, processingModel, SystemEntityType.ACTIVITY);
    }

    public IUniqueId invoke(ActivityRequest request) throws ProcessingModelException {
        // TODO: handle new activity occurrence creation, decalibration of the arguments, argument and route checks and forward to activity handler
        // Static check failures (decalibration errors, arguments, route) generate an error message and throw an exception. The
        // ActivityOccurrenceProcessor is created only when the activity invocation must be forwarded to the activity handler.
        throw new UnsupportedOperationException();
    }

    public IUniqueId create(ActivityRequest request, ActivityProgress progress) throws ProcessingModelException {
        // TODO: handle new activity occurrence creation and state setup
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AbstractDataItem> process(ActivityProgress input) throws ProcessingModelException {
        // TODO: handle activity occurrence progress
        return null;
    }

    @Override
    public List<AbstractDataItem> evaluate() throws ProcessingModelException {
        // TODO: handle re-evaluation of all the activity occurrences for this activity
        return null;
    }

    public List<AbstractDataItem> evaluate(IUniqueId occurrenceId) {
        // TODO: handle re-evaluation of a single occurrence
        return null;
    }
}
