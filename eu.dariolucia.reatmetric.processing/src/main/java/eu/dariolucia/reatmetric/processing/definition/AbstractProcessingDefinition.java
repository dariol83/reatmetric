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

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class AbstractProcessingDefinition implements Serializable {

    @XmlID
    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(IntToStringAdapter.class)
    private Integer id;

    @XmlAttribute(required = true)
    private String location;

    @XmlAttribute
    private String description = "";

    @XmlTransient
    private boolean mirrored = false;

    protected AbstractProcessingDefinition() {
        // None
    }

    protected AbstractProcessingDefinition(int id, String description, String location) {
        this.id = id;
        this.description = description;
        this.location = location;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Define whether this object is a mirrored object, i.e. the status can only be reported to the processing model from the mirror()
     * operation. A mirrored object is an object, whose state is not computed by the processing model. The only operation that the processing model
     * can do, is to ignore the update ({@link eu.dariolucia.reatmetric.api.model.Status#IGNORED}.
     *
     * Processing-related updates, i.e. parameter injection, event reports and activity requests/reports are ignored and a warning is raised in such cases.
     *
     * @return true if the object is a mirrored one, false otherwise.
     */
    public boolean isMirrored() {
        return mirrored;
    }

    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
    }

    /**
     * Runtime operation to activate definition preloading - e.g for expressions or heavy computations
     */
    public abstract void preload() throws Exception;
}
