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

package eu.dariolucia.reatmetric.api.common;

import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;

import java.io.Serializable;

/**
 * Objects of this class are descriptions of the specific system entity they refer to.
 */
public abstract class AbstractSystemEntityDescriptor implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final SystemEntityPath path;
    private final int externalId;
    private final SystemEntityType type;

    protected AbstractSystemEntityDescriptor(SystemEntityPath path, int externalId, SystemEntityType type) {
        this.path = path;
        this.externalId = externalId;
        this.type = type;
    }

    /**
     * Return the {@link SystemEntityPath}, the system entity descriptor refers to.
     *
     * @return the path of the system entity
     */
    public SystemEntityPath getPath() {
        return path;
    }

    /**
     * Return the ID of the system entity, the system entity descriptor refers to.
     *
     * @return the ID
     */
    public int getExternalId() {
        return externalId;
    }

    /**
     * Return the {@link SystemEntityType} of the system entity.
     *
     * @return the type
     */
    public SystemEntityType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "{" +
                "externalId=" + getExternalId() +
                ", path=" + getPath() +
                ", type=" + getType() +
                "}";
    }
}
