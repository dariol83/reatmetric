/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class AbstractInitialisationConfiguration {

    @XmlAttribute(name = "look-back-time")
    private int lookBackTime = 3600; // Number of seconds to look back (increase start-up performance)

    public int getLookBackTime() {
        return lookBackTime;
    }

    public void setLookBackTime(int lookBackTime) {
        this.lookBackTime = lookBackTime;
    }
}
