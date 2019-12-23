/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.input;

public final class ActivityArgument {

    public static ActivityArgument ofSource(String name, Object sourceValue) {
        return of(name, sourceValue, null, false);
    }

    public static ActivityArgument ofEngineering(String name, Object engValue) {
        return of(name, null, engValue, true);
    }

    public static ActivityArgument of(String name, Object sourceValue, Object engValue, boolean engineering) {
        return new ActivityArgument(name, sourceValue, engValue, engineering);
    }

    private final String name;

    private final Object rawValue;

    private final Object engValue;

    private final boolean engineering;

    public ActivityArgument(String name, Object rawValue, Object engValue, boolean engineering) {
        this.name = name;
        this.rawValue = rawValue;
        this.engValue = engValue;
        this.engineering = engineering;
    }

    public String getName() {
        return name;
    }

    public Object getRawValue() {
        return rawValue;
    }

    public Object getEngValue() {
        return engValue;
    }

    public boolean isEngineering() {
        return engineering;
    }
}
