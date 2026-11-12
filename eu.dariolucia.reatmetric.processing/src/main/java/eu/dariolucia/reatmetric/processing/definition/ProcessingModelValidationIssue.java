/*
 * Copyright (c)  2026 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.processing.definition;

import java.io.Serializable;

/**
 * Validation issue detected while checking a {@link ProcessingDefinition}.
 */
public final class ProcessingModelValidationIssue implements Serializable {

    private final ProcessingModelValidationIssueType type;
    private final String location;
    private final String message;

    /**
     * Create a validation issue with its type, model location and explanation.
     *
     * @param type the type of validation issue
     * @param location the model location where the issue was detected
     * @param message the human-readable explanation of the issue
     */
    public ProcessingModelValidationIssue(ProcessingModelValidationIssueType type, String location, String message) {
        this.type = type;
        this.location = location;
        this.message = message;
    }

    /**
     * Return the type of validation problem.
     *
     * @return the validation issue type
     */
    public ProcessingModelValidationIssueType getType() {
        return type;
    }

    /**
     * Return the model location where the validation problem was detected.
     *
     * @return the model location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Return the human-readable validation problem explanation.
     *
     * @return the validation problem explanation
     */
    public String getMessage() {
        return message;
    }

    /**
     * Return the validation issue in a compact, single-line format.
     *
     * @return the formatted validation issue
     */
    @Override
    public String toString() {
        return "[" + type + "] " + toSingleLine(location) + ": " + toSingleLine(message);
    }

    private static String toSingleLine(String value) {
        return value == null ? null : value.replace('\r', ' ').replace('\n', ' ');
    }
}
