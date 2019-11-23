/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class SymbolDefinition {

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlIDREF
    @XmlAttribute(name = "reference", required = true)
    private AbstractProcessingDefinition reference;

    public SymbolDefinition() {
    }

    public SymbolDefinition(String name, AbstractProcessingDefinition reference) {
        this.name = name;
        this.reference = reference;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AbstractProcessingDefinition getReference() {
        return reference;
    }

    public void setReference(AbstractProcessingDefinition reference) {
        this.reference = reference;
    }
}
