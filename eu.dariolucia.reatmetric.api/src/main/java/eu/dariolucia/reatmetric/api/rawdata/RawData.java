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


package eu.dariolucia.reatmetric.api.rawdata;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;

import java.io.Serializable;
import java.time.Instant;

/**
 * An instance of this class represents a raw data unit managed by ReatMetric.
 * <p>
 * Raw data is a semi-opaque object: ReatMetric stores and distributes the payload and the associated metadata, but it does
 * not interpret the payload contents directly. Interpretation, when available, is provided by raw data renderers registered
 * by drivers. Typical examples are protocol data units, frames, packets, or driver/module state snapshots that must be
 * distributed to subscribers and optionally persisted.
 * <p>
 * Each raw data item carries generation and reception timestamps, routing/source metadata, quality information, an optional
 * relationship to another item, optional binary contents, an optional extension object, and a {@code live} flag. A live raw
 * data item is one received or produced as part of the current live processing flow. A non-live raw data item is repatriated
 * data: data generated elsewhere or in the past, stored outside ReatMetric, and later brought into the system.
 * <p>
 * Objects of this class are immutable.
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

    private final String handler;

    private final boolean live;

    /**
     * Constructor of the class. The constructed raw data item is marked as live.
     *
     * @param internalId the internal ID identifying the raw data item
     * @param generationTime the generation time of the raw data item
     * @param name the raw data name
     * @param type the raw data type
     * @param route the route associated to the raw data item, can be null
     * @param source the source associated to the raw data item, can be null
     * @param quality the quality of the raw data item
     * @param relatedItem the unique ID of a related item, can be null
     * @param contents the raw data binary contents, can be null
     * @param receptionTime the time when the raw data item was received or became available to ReatMetric
     * @param handler the function that handled or created this raw data item
     * @param extension an extension object, can be null
     */
    public RawData(IUniqueId internalId, Instant generationTime, String name, String type, String route, String source, Quality quality, IUniqueId relatedItem, byte[] contents, Instant receptionTime, String handler, Object extension) {
        this(internalId, generationTime, name, type, route, source, quality, relatedItem, contents, receptionTime, handler, true, extension);
    }

    /**
     * Constructor of the class.
     *
     * @param internalId the internal ID identifying the raw data item
     * @param generationTime the generation time of the raw data item
     * @param name the raw data name
     * @param type the raw data type
     * @param route the route associated to the raw data item, can be null
     * @param source the source associated to the raw data item, can be null
     * @param quality the quality of the raw data item
     * @param relatedItem the unique ID of a related item, can be null
     * @param contents the raw data binary contents, can be null
     * @param receptionTime the time when the raw data item was received or became available to ReatMetric
     * @param handler the function that handled or created this raw data item
     * @param live true if the raw data item was received or produced by the live processing flow, false if it was repatriated
     * @param extension an extension object, can be null
     */
    public RawData(IUniqueId internalId, Instant generationTime, String name, String type, String route, String source, Quality quality, IUniqueId relatedItem, byte[] contents, Instant receptionTime, String handler, boolean live, Object extension) {
        super(internalId, generationTime, extension);
        this.name = name;
        this.type = type;
        this.route = route;
        this.receptionTime = receptionTime;
        this.source = source;
        this.quality = quality;
        this.contents = contents;
        this.relatedItem = relatedItem;
        this.handler = handler;
        this.live = live;
    }

    /**
     * Return the raw data name.
     *
     * @return the raw data name
     */
    public String getName() {
        return name;
    }

    /**
     * Return the raw data type.
     *
     * @return the raw data type
     */
    public String getType() {
        return type;
    }

    /**
     * Return the route associated to this raw data item.
     * It can be null if not specified.
     *
     * @return the raw data route, or null if not specified
     */
    public String getRoute() {
        return route;
    }

    /**
     * Return the reception time of this raw data item. The reception time is the time when the raw data item was received
     * or became available to ReatMetric.
     *
     * @return the reception time
     */
    public Instant getReceptionTime() {
        return receptionTime;
    }

    /**
     * Return the source associated to this raw data item.
     * It can be null if not specified.
     *
     * @return the raw data source, or null if not specified
     */
    public String getSource() {
        return source;
    }

    /**
     * Return the quality of this raw data item.
     *
     * @return the raw data quality
     */
    public Quality getQuality() {
        return quality;
    }

    /**
     * Return the binary contents of this raw data item.
     * It can be null if no contents are available, or if the item was retrieved with a filter requesting metadata only.
     *
     * @return the raw data binary contents, or null if not available
     */
    public byte[] getContents() {
        return contents;
    }

    /**
     * Return whether the binary contents are available in this object.
     *
     * @return true if the contents are available, otherwise false
     */
    public boolean isContentsSet() {
        return contents != null;
    }

    /**
     * Return the unique ID of a related data item.
     * It can be null if not specified.
     *
     * @return the related item unique ID, or null if not specified
     */
    public IUniqueId getRelatedItem() {
        return relatedItem;
    }


    /**
     * Return the function that handled or created this raw data item in the ReatMetric system.
     *
     * @return the raw data handler
     */
    public String getHandler() {
        return handler;
    }

    /**
     * Return whether this raw data item belongs to the live processing flow.
     *
     * @return true for live data, false for repatriated data
     */
    public boolean isLive() {
        return live;
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
                ", handler=" + handler +
                ", live=" + live +
                ", internalId=" + internalId +
                '}';
    }
}
