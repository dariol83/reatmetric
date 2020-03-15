/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.definition;

import javax.xml.bind.annotation.*;
import java.time.Instant;

// TODO: Rename to TmPacketConfiguration
@XmlAccessorType(XmlAccessType.FIELD)
public class PusConfiguration {

    // TODO: group the time/CRC configuration in one class and introduce a default and a per-APID configuration
    @XmlAttribute(name = "source_field_length")
    private int sourceLength = 0;

    @XmlAttribute(name = "tc_spare_length")
    private int tcSpareLength = 0;

    @XmlAttribute(name = "destination_field_length")
    private int destinationLength = 0;

    @XmlAttribute(name = "packet_subcounter_length")
    private int packetSubCounterLength = 0;

    @XmlAttribute(name = "tm_spare_length")
    private int tmSpareLength = 0;

    @XmlAttribute(name = "explicit_p_field")
    private boolean explicitPField = false;

    @XmlAttribute(name = "obt_epoch")
    private Instant epoch = null;

    @XmlAttribute(name = "tm_pec_present")
    private PacketErrorControlType tmPecPresent = PacketErrorControlType.NONE;

    @XmlAttribute(name = "parameter_id_offset")
    private int parameterIdOffset = 0;

    @XmlAttribute(name = "event_id_offset")
    private int eventIdOffset = 0;

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

    public Instant getEpoch() {
        return epoch;
    }

    public void setEpoch(Instant epoch) {
        this.epoch = epoch;
    }

    public PacketErrorControlType getTmPecPresent() {
        return tmPecPresent;
    }

    public void setTmPecPresent(PacketErrorControlType tmPecPresent) {
        this.tmPecPresent = tmPecPresent;
    }

    public ObtConfiguration getObtConfiguration() {
        return obtConfiguration;
    }

    public void setObtConfiguration(ObtConfiguration obtConfiguration) {
        this.obtConfiguration = obtConfiguration;
    }

    public int getSourceLength() {
        return sourceLength;
    }

    public void setSourceLength(int sourceLength) {
        this.sourceLength = sourceLength;
    }

    public int getTcSpareLength() {
        return tcSpareLength;
    }

    public void setTcSpareLength(int tcSpareLength) {
        this.tcSpareLength = tcSpareLength;
    }

    public int getPacketSubCounterLength() {
        return packetSubCounterLength;
    }

    public void setPacketSubCounterLength(int packetSubCounterLength) {
        this.packetSubCounterLength = packetSubCounterLength;
    }

    public int getTmSpareLength() {
        return tmSpareLength;
    }

    public void setTmSpareLength(int tmSpareLength) {
        this.tmSpareLength = tmSpareLength;
    }

    public int getParameterIdOffset() {
        return parameterIdOffset;
    }

    public void setParameterIdOffset(int parameterIdOffset) {
        this.parameterIdOffset = parameterIdOffset;
    }

    public int getEventIdOffset() {
        return eventIdOffset;
    }

    public void setEventIdOffset(int eventIdOffset) {
        this.eventIdOffset = eventIdOffset;
    }
}
