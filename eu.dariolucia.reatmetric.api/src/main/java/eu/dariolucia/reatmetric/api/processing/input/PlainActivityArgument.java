/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.api.processing.input;

/**
 * Represents a plain activity argument with source and engineering values.
 * This is an immutable class that extends AbstractActivityArgument.
 */
public final class PlainActivityArgument extends AbstractActivityArgument {

    /**
     * Creates a PlainActivityArgument with a source value.
     *
     * @param name the argument name
     * @param sourceValue the source value
     * @return a new PlainActivityArgument instance
     */
    public static PlainActivityArgument ofSource(String name, Object sourceValue) {
        return of(name, sourceValue, null, false);
    }

    /**
     * Creates a PlainActivityArgument with an engineering value.
     *
     * @param name the argument name
     * @param engValue the engineering value
     * @return a new PlainActivityArgument instance
     */
    public static PlainActivityArgument ofEngineering(String name, Object engValue) {
        return of(name, null, engValue, true);
    }

    /**
     * Creates a PlainActivityArgument with all values specified.
     *
     * @param name the argument name
     * @param sourceValue the source value, can be null
     * @param engValue the engineering value, can be null
     * @param engineering whether this is an engineering value
     * @return a new PlainActivityArgument instance
     */
    public static PlainActivityArgument of(String name, Object sourceValue, Object engValue, boolean engineering) {
        return new PlainActivityArgument(name, sourceValue, engValue, engineering);
    }

    /** The raw (source) value. */
    private final Object rawValue;

    /** The engineering value. */
    private final Object engValue;

    /** Whether this argument uses engineering values. */
    private final boolean engineering;

    /**
     * Constructs a new PlainActivityArgument.
     *
     * @param name the argument name
     * @param rawValue the source value, can be null
     * @param engValue the engineering value, can be null
     * @param engineering whether this is an engineering value
     */
    public PlainActivityArgument(String name, Object rawValue, Object engValue, boolean engineering) {
        super(name);
        this.rawValue = rawValue;
        this.engValue = engValue;
        this.engineering = engineering;
    }

    /**
     * Returns the raw (source) value.
     *
     * @return the raw value, may be null
     */
    public Object getRawValue() {
        return rawValue;
    }

    /**
     * Returns the engineering value.
     *
     * @return the engineering value, may be null
     */
    public Object getEngValue() {
        return engValue;
    }

    /**
     * Returns whether this argument uses engineering values.
     *
     * @return true if this is an engineering value, false otherwise
     */
    public boolean isEngineering() {
        return engineering;
    }
}
