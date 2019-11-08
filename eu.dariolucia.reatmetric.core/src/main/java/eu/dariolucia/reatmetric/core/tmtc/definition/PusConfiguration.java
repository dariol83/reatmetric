/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.tmtc.definition;

import javax.xml.bind.annotation.*;
import java.util.Date;

@XmlAccessorType(XmlAccessType.FIELD)
public class PusConfiguration {

    @XmlAttribute(name = "destination_field_length")
    private int destinationLength = 1;

    @XmlAttribute(name = "explicit_p_field")
    private boolean explicitPField = false;

    @XmlAttribute(name = "obt_epoch")
    private Date epoch = null;

    @XmlAttribute(name = "pec_present")
    private boolean pecPresent = true;

    @XmlElements({
            @XmlElement(name = "obt_cuc_config", type = CucConfiguration.class),
            @XmlElement(name = "obt_cds_config", type = CdsConfiguration.class)
    })
    private ObtConfiguration obtConfiguration = new CucConfiguration();

    public PusConfiguration() {
    }

    public int getDestinationLength() {
        return destinationLength;
    }

    public void setDestinationLength(int destinationLength) {
        this.destinationLength = destinationLength;
    }

    public boolean isExplicitPField() {
        return explicitPField;
    }

    public void setExplicitPField(boolean explicitPField) {
        this.explicitPField = explicitPField;
    }

    public Date getEpoch() {
        return epoch;
    }

    public void setEpoch(Date epoch) {
        this.epoch = epoch;
    }

    public boolean isPecPresent() {
        return pecPresent;
    }

    public void setPecPresent(boolean pecPresent) {
        this.pecPresent = pecPresent;
    }

    public ObtConfiguration getObtConfiguration() {
        return obtConfiguration;
    }

    public void setObtConfiguration(ObtConfiguration obtConfiguration) {
        this.obtConfiguration = obtConfiguration;
    }
}
