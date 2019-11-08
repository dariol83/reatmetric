/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.tmtc.impl;

import java.time.Instant;

public class PusCharacteristics {

    private final Instant generationTime;

    private final int destinationId;

    private final int pusType;

    private final int pusSubType;

    private final int dataOffset;

    public PusCharacteristics(Instant generationTime, int destinationId, int pusType, int pusSubType, int dataOffset) {
        this.generationTime = generationTime;
        this.destinationId = destinationId;
        this.pusType = pusType;
        this.pusSubType = pusSubType;
        this.dataOffset = dataOffset;
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public Instant getGenerationTime() {
        return generationTime;
    }

    public int getDestinationId() {
        return destinationId;
    }

    public int getPusType() {
        return pusType;
    }

    public int getPusSubType() {
        return pusSubType;
    }
}
