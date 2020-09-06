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

package eu.dariolucia.reatmetric.driver.automation.common;

import eu.dariolucia.reatmetric.processing.definition.ExpressionDefinition;

public class Constants {
    // Activity Type
    public static final String T_SCRIPT_TYPE = "SCRIPT";

    // First argument -> file name
    public static final String ARGUMENT_FILE_NAME = "FILENAME";

    // Bindings
    public static final String BINDING_SCRIPT_MANAGER = "_scriptManager";

    // API resource files
    public static final String API_JS_RESOURCE_FILE = "init.js";
    public static final String API_GROOVY_RESOURCE_FILE = "init.groovy";
    public static final String API_PYTHON_RESOURCE_FILE = "init.py";

    // Supported extensions
    public static final String JS_EXTENSION = "js";
    public static final String GROOVY_EXTENSION = "groovy";
    public static final String PYTHON_EXTENSION = "py";

    // Automation engine route
    public static final String AUTOMATION_ROUTE = "Automation Engine";

    // Python result object name
    public static final String PYTHON_RESULT_NAME = ExpressionDefinition.PYTHON_RESULT_NAME;
}
