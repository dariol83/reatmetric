/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.input;

public final class ActivityArgument {

    public static ActivityArgument ofSource(String name, Object sourceValue) {
        return of(name, sourceValue, null);
    }

    public static ActivityArgument ofEngineering(String name, Object engValue) {
        return of(name, null, engValue);
    }

    public static ActivityArgument of(String name, Object sourceValue, Object engValue) {
        return new ActivityArgument(name, sourceValue, engValue);
    }

    private final String name;

    private final Object sourceValue;

    private final Object engValue;

    public ActivityArgument(String name, Object sourceValue, Object engValue) {
        this.name = name;
        this.sourceValue = sourceValue;
        this.engValue = engValue;
    }

    public String getName() {
        return name;
    }

    public Object getSourceValue() {
        return sourceValue;
    }

    public Object getEngValue() {
        return engValue;
    }
}
