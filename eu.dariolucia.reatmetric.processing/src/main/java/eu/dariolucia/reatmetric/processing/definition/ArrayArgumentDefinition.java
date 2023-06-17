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

import jakarta.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ArrayArgumentDefinition extends AbstractArgumentDefinition implements Serializable {

    @XmlAttribute(name = "argument_expander", required = true)
    private String argumentExpander;

    @XmlElements({
            @XmlElement(name="argument",type= PlainArgumentDefinition.class),
            @XmlElement(name="array",type= ArrayArgumentDefinition.class)
    })
    private List<AbstractArgumentDefinition> elements = new LinkedList<>();

    public ArrayArgumentDefinition() {
    }

    public ArrayArgumentDefinition(String name, String description, String argumentExpander, List<AbstractArgumentDefinition> elements) {
        super(name, description);
        this.elements = elements;
        this.argumentExpander = argumentExpander;
    }

    public List<AbstractArgumentDefinition> getElements() {
        return elements;
    }

    public void setElements(List<AbstractArgumentDefinition> elements) {
        this.elements = elements;
    }

    /**
     * Returns the name of the argument in the activity invocation, whose actual value indicates the number of repetitions in this group
     *
     * @return the expander argument name
     */
    public String getArgumentExpander() {
        return argumentExpander;
    }

    public void setArgumentExpander(String argumentExpander) {
        this.argumentExpander = argumentExpander;
    }
}
