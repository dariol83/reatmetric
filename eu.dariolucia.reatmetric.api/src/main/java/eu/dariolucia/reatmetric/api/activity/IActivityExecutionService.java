/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.activity;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;

import java.util.List;

public interface IActivityExecutionService {

    IUniqueId startActivity(ActivityRequest request) throws ReatmetricException;

    IUniqueId createActivity(ActivityRequest request, ActivityProgress currentProgress) throws ReatmetricException;

    // TODO add a way to set parameter value
    // IUniqueId setParameter(SetParameterRequest request) throws ReatmetricException

    void purgeActivities(List<Pair<Integer, IUniqueId>> activityOccurrenceIds) throws ReatmetricException;

}
