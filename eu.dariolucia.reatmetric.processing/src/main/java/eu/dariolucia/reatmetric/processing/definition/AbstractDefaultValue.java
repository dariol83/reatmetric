/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class AbstractDefaultValue {

    @XmlAttribute(name = "type", required = true)
    private DefaultValueType type;

    public AbstractDefaultValue() {
    }

    public AbstractDefaultValue(DefaultValueType type) {
        this.type = type;
    }

    public DefaultValueType getType() {
        return type;
    }

    public void setType(DefaultValueType type) {
        this.type = type;
    }
}
