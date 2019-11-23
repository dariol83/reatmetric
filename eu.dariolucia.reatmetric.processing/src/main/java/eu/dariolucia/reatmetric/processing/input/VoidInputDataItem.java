/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.input;

/**
 * This class is used for system entity processors that do not need any input data item to work, as they use only
 * standard operations.
 */
public final class VoidInputDataItem extends AbstractInputDataItem {

    private static final VoidInputDataItem INSTANCE = new VoidInputDataItem();

    public static VoidInputDataItem instance() {
        return INSTANCE;
    }

    private VoidInputDataItem() {
        // private constructor, nothing to do here
    }
}
