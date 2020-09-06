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

package eu.dariolucia.reatmetric.processing.definition;

/**
 * The type of language of an expression.
 */
public enum ExpressionDialect {
    /**
     * The expression will be evaluated using the Javascript interpreter.
     */
    JS,
    /**
     * The expression will be evaluated using the Groovy interpreter.
     */
    GROOVY,
    /**
     * The expression will be evaluated using the Python interpreter.
     *
     * Important: the support of Python for expressions is inherently tricky and works as expected for single expressions. A full length block with statements,
     * if, for, complex function calls and other blocks will not return any value as it will be compiled for 'exec'ution rather than
     * for 'eval'uation: https://stackoverflow.com/questions/2220699/whats-the-difference-between-eval-exec-and-compile
     * Reatmetric uses the approach described here: https://stackoverflow.com/questions/1887320/get-data-back-from-jython-scripts-using-jsr-223
     * The result is fetched by getting the variable _result at the end of the expression evaluation, so make sure to set it accordingly.
     */
    PYTHON
}
