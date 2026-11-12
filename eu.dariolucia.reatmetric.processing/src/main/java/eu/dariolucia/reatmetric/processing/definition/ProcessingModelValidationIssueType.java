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

/**
 * Enumeration of validation problems detected in a processing model definition.
 */
public enum ProcessingModelValidationIssueType {
    DUPLICATE_ACTIVITY_ARGUMENT_NAME,
    DUPLICATE_ENTITY_ID,
    DUPLICATE_ENTITY_PATH,
    INVALID_XY_CALIBRATION_POINTS,
    UNSUPPORTED_DEFAULT_VALUE,
    UNPARSEABLE_DEFAULT_VALUE,
    NULL_DEFAULT_VALUE,
    MISSING_REFERENCED_ENTITY,
    GROOVY_EXPRESSION_SYNTAX_ERROR,
    UNSUPPORTED_EXPRESSION_DIALECT
}
