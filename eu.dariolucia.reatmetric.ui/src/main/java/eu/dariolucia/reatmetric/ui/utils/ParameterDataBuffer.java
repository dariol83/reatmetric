/*
 * Copyright (c)  2026 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.ui.utils;

import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 *
 */
public class ParameterDataBuffer {

    private final SystemEntityPath path;
    private final Queue<ParameterData> buffer = new LinkedList<>();
    private final int nbItems;
    private final ParameterDataFilter filter;

    private RetrievalDirection lastDirection = null;
    private boolean endOfStream = false;

    public ParameterDataBuffer(SystemEntityPath path, int nbItems) {
        this.path = path;
        this.nbItems = nbItems;
        this.filter = new ParameterDataFilter(null, Collections.singletonList(path), null, null, null, null);
    }

    public ParameterData evaluateNext(ParameterData currentData, RetrievalDirection direction) throws ReatmetricException {
        if(lastDirection != direction) {
            // Invalidate
            buffer.clear();
            lastDirection = direction;
            endOfStream = false;

        }
        // If end of stream, return null
        if(endOfStream) {
            return null;
        }
        // Same direction, retrieve
        if(buffer.isEmpty()) {
            List<ParameterData> nextStates = doRetrieve(currentData, nbItems, direction, filter);
            if(nextStates.isEmpty()) {
                // No more states in this direction
                endOfStream = true;
                return null;
            } else {
                buffer.addAll(nextStates);
            }
        }
        return buffer.peek();
    }

    public ParameterData pop() {
        if(buffer.isEmpty()) {
            throw new IllegalStateException("Buffer empty for paramter " + path + ": usage error");
        }
        return buffer.remove();
    }

    public void reset() {
        // Invalidate
        buffer.clear();
        lastDirection = null;
        endOfStream = false;
    }

    private List<ParameterData> doRetrieve(ParameterData startItem, int n, RetrievalDirection direction, ParameterDataFilter filter) throws ReatmetricException {
        try {
            return ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().retrieve(startItem, n, direction, filter);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }
}
