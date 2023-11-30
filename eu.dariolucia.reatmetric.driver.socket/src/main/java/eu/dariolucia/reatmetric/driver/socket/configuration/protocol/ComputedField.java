/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.socket.configuration.protocol;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import jakarta.xml.bind.annotation.*;

import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class ComputedField {

    // Name of the field of the message
    @XmlAttribute(required = true)
    private String field;

    @XmlValue
    private String expression;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    /* ***************************************************************
     * Internal operations
     * ***************************************************************/

    @XmlTransient
    private GroovyShell groovyShell;
    @XmlTransient
    private Binding groovyBinding;
    @XmlTransient
    private Script groovyScript;

    public void initialise() {
        groovyBinding = new Binding();
        groovyShell = new GroovyShell();
        groovyScript = groovyShell.parse(expression);
        groovyScript.setBinding(groovyBinding);
    }

    public Object compute(String message, String secondaryId, Map<String, Object> mappedArguments) throws ReatmetricException {
        // TODO: replace space and dash with underscore
        for(Map.Entry<String, Object> entry : mappedArguments.entrySet()) {
            groovyBinding.setProperty(entry.getKey(), entry.getValue());
        }
        try {
            return groovyScript.run();
        } catch (Exception e) {
            throw new ReatmetricException(e);
        }
    }
}
