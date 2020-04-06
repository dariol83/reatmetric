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
