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


package eu.dariolucia.reatmetric.api.parameters;

/**
 * Enumeration of validity states for parameter data.
 * The validity indicates the quality or reliability of the parameter value.
 *
 * @author dario
 */
public enum Validity {
    /**
     * The parameter value is valid and reliable.
     */
    VALID("Valid"),
    
    /**
     * The parameter value is invalid and should not be used.
     */
    INVALID("Invalid"),
    
    /**
     * The parameter is disabled and not providing valid data.
     */
    DISABLED("Disabled"),
    
    /**
     * The validity of the parameter value is unknown.
     */
    UNKNOWN("Unknown"),
    
    /**
     * An error occurred while determining the validity.
     */
    ERROR("Error");

    /**
     * The printable name of this validity state.
     */
    private final String printableName;

    /**
     * Constructor for a validity state.
     *
     * @param printableName the printable name of this validity state, cannot be null
     */
    Validity(String printableName) {
        this.printableName = printableName;
    }

    /**
     * Returns the printable name of this validity state.
     *
     * @return the printable name (cannot be null)
     */
    public String getPrintableName() {
        return printableName;
    }
}
