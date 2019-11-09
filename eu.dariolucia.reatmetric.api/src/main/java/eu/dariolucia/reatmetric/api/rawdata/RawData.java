/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.rawdata;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;

/**
 *
 * @author dario
 */
public final class RawData extends AbstractDataItem implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -4888757815934729601L;

	private final String name;

    private final String type;
    
    private final String route;

    private final Instant receptionTime;

    private final String source;

    private final Quality quality;

    public RawData(IUniqueId internalId, String name, String type, String route, Instant generationTime, Instant receptionTime, String source, Quality quality, Object[] additionalFields) {
        super(internalId, generationTime, additionalFields);
        this.name = name;
        this.type = type;
        this.route = route;
        this.receptionTime = receptionTime;
        this.source = source;
        this.quality = quality;
    }

    public String getName() {
        return name;
    }
    
    public String getType() {
        return type;
    }

    public String getRoute() {
        return route;
    }

    public Instant getReceptionTime() {
        return receptionTime;
    }

    public String getSource() {
        return source;
    }

    public Quality getQuality() {
        return quality;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 73 * hash + Objects.hashCode(this.name);
        hash = 73 * hash + Objects.hashCode(this.type);
        hash = 73 * hash + Objects.hashCode(this.route);
        hash = 73 * hash + Objects.hashCode(this.receptionTime);
        hash = 73 * hash + Objects.hashCode(this.generationTime);
        hash = 73 * hash + Objects.hashCode(this.source);
        hash = 73 * hash + Objects.hashCode(this.quality);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RawData other = (RawData) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.receptionTime, other.receptionTime)) {
            return false;
        }
        if (!Objects.equals(this.generationTime, other.generationTime)) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (!Objects.equals(this.route, other.route)) {
            return false;
        }
        if (!Objects.equals(this.source, other.source)) {
            return false;
        }
        if (this.quality != other.quality) {
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "RawData [" + "name=" + name + ", type=" + type + ", route=" + route + ", generationTime=" + generationTime + ", receptionTime=" + receptionTime + ", source=" + source + ", quality=" + quality + ']';
    }
    
}
