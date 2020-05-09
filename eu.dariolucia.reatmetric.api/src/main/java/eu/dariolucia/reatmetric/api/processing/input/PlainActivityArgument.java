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

public final class PlainActivityArgument extends AbstractActivityArgument {

    public static PlainActivityArgument ofSource(String name, Object sourceValue) {
        return of(name, sourceValue, null, false);
    }

    public static PlainActivityArgument ofEngineering(String name, Object engValue) {
        return of(name, null, engValue, true);
    }

    public static PlainActivityArgument of(String name, Object sourceValue, Object engValue, boolean engineering) {
        return new PlainActivityArgument(name, sourceValue, engValue, engineering);
    }

    private final Object rawValue;

    private final Object engValue;

    private final boolean engineering;

    public PlainActivityArgument(String name, Object rawValue, Object engValue, boolean engineering) {
        super(name);
        this.rawValue = rawValue;
        this.engValue = engValue;
        this.engineering = engineering;
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
