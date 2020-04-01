/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.processors.builders;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

public abstract class AbstractDataItemBuilder<T extends AbstractDataItem> {

    protected final int id;

    protected final SystemEntityPath path;

    protected boolean changedSinceLastBuild;

    public AbstractDataItemBuilder(int id, SystemEntityPath path) {
        this.id = id;
        this.path = path;
        this.changedSinceLastBuild = false;
    }

    public boolean isChangedSinceLastBuild() {
        return changedSinceLastBuild;
    }

    public abstract T build(IUniqueId updateId);

    public abstract void setInitialisation(T item);
}
