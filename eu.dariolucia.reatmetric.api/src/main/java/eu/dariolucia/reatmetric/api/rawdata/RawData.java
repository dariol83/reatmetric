/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.rawdata;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;

import java.io.Serializable;
import java.time.Instant;

/**
 *
 * @author dario
 */
public final class RawData extends AbstractDataItem implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String name;

    private final String type;
    
    private final String route;

    private final Instant receptionTime;

    private final String source;

    private final Quality quality;

    private final IUniqueId relatedItem;

    private final byte[] contents;

    public RawData(IUniqueId internalId, Instant generationTime, String name, String type, String route, String source, Quality quality, IUniqueId relatedItem, byte[] contents, Instant receptionTime, Object extension) {
        super(internalId, generationTime, extension);
        this.name = name;
        this.type = type;
        this.route = route;
        this.receptionTime = receptionTime;
        this.source = source;
        this.quality = quality;
        this.contents = contents;
        this.relatedItem = relatedItem;
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

    public byte[] getContents() {
        return contents;
    }

    public boolean isContentsSet() {
        return contents != null;
    }

    public IUniqueId getRelatedItem() {
        return relatedItem;
    }

    @Override
    public String toString() {
        return "RawData{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", route='" + route + '\'' +
                ", receptionTime=" + receptionTime +
                ", source='" + source + '\'' +
                ", quality=" + quality +
                ", relatedItem=" + relatedItem +
                ", generationTime=" + generationTime +
                ", internalId=" + internalId +
                '}';
    }
}
