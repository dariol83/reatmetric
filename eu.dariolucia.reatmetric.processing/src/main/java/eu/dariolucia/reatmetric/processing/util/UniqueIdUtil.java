/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.util;

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.rawdata.RawData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class UniqueIdUtil {

    private static final long START_TIME;

    private static final Map<Class<? extends AbstractDataItem>, AtomicLong> SEQUENCERS;

    static {
        START_TIME = System.currentTimeMillis();
        SEQUENCERS = new HashMap<>();
        SEQUENCERS.put(RawData.class, new AtomicLong(0));
        SEQUENCERS.put(ParameterData.class, new AtomicLong(0));
        SEQUENCERS.put(OperationalMessage.class, new AtomicLong(0));
        SEQUENCERS.put(AlarmParameterData.class, new AtomicLong(0));
        SEQUENCERS.put(EventData.class, new AtomicLong(0));
    }

    public static IUniqueId generateNextId(Class<? extends AbstractDataItem> type) {
        long id = SEQUENCERS.get(type).getAndIncrement();
        return new LongUniqueId(id);
    }
}
