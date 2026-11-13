/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.api.scheduler;

import java.io.Serializable;

/**
 * Represents the state of a bot (automated process).
 * Contains the bot name, state, state name, and enabled status.
 */
public final class BotStateData implements Serializable {

    /** The name of the bot. */
    private final String name;

    /** The numeric state of the bot. */
    private final int state;

    /** The name of the state. */
    private final String stateName;

    /** Whether the bot is enabled. */
    private final boolean enabled;

    /**
     * Constructs a new BotStateData.
     *
     * @param name the name of the bot
     * @param state the numeric state of the bot
     * @param stateName the name of the state
     * @param enabled whether the bot is enabled
     */
    public BotStateData(String name, int state, String stateName, boolean enabled) {
        this.name = name;
        this.state = state;
        this.stateName = stateName;
        this.enabled = enabled;
    }

    /**
     * Returns the name of the bot.
     *
     * @return the bot name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the numeric state of the bot.
     *
     * @return the state
     */
    public int getState() {
        return state;
    }

    /**
     * Returns the name of the state.
     *
     * @return the state name
     */
    public String getStateName() {
        return stateName;
    }

    /**
     * Returns whether the bot is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    /**
     * Returns a string representation of this BotStateData.
     *
     * @return the string representation
     */
    public String toString() {
        return "BotStateData{" +
                "name='" + name + '\'' +
                ", state=" + state +
                ", stateName='" + stateName + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
