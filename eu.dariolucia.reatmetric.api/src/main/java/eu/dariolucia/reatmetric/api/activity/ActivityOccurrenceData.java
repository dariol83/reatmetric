/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.activity;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ActivityOccurrenceData extends AbstractDataItem {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final int externalId;

    private final IUniqueId occurrenceId;

    private final String name;

    private final SystemEntityPath path;

    private final Map<String, Object> arguments;

    private final Map<String, String> properties;

    private final List<ActivityOccurrenceReport> progressReports;

    private final Instant executionTime;

    private final String route;

    private final ActivityOccurrenceState currentState;

    private final boolean timedOut;

    private final boolean terminated;




}
