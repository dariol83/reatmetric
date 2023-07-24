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


package eu.dariolucia.reatmetric.api.messages;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.model.SystemEntity;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This class allows to filter/subscribe/retrieve acknowledged messages.
 *
 * Objects of this class are immutable.
 */
public final class AcknowledgedMessageFilter extends AbstractDataItemFilter<AcknowledgedMessage> implements Serializable {

    /**
	 *
	 */
	private static final long serialVersionUID = 1L;

    private final Set<String> userList;

    private final Set<AcknowledgementState> stateList;

    /**
     * The constructor of the acknowledged message filter.
     *
     * @param userList the list of users to select. It can be null: if so, all users are selected
     * @param stateList the list of acknowledged message states to select. It can be null: if so, all states are selected.
     */
    public AcknowledgedMessageFilter(Collection<String> userList, Collection<AcknowledgementState> stateList) {
        if(userList != null) {
            this.userList = Collections.unmodifiableSet(new LinkedHashSet<>(userList));
        } else {
            this.userList = null;
        }
        if(stateList != null) {
            this.stateList = Collections.unmodifiableSet(new LinkedHashSet<>(stateList));
        } else {
            this.stateList = null;
        }
    }

    /**
     * The set of users to select: an acknowledged message is selected if its user is not null and if it is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified users
     */
    public Set<String> getUserList() {
        return userList;
    }

    /**
     * The set of acknowledged message states to select: an acknowledged message is selected if its state is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified states
     */
    public Set<AcknowledgementState> getStateList() {
        return stateList;
    }

    @Override
    public boolean isClear() {
        return this.userList == null && this.stateList == null;
    }

    @Override
    public boolean test(AcknowledgedMessage item) {
        if(stateList != null && !stateList.contains(item.getState())) {
            return false;
        }
        if(userList != null && !userList.contains(item.getUser())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean select(SystemEntity entity) {
        return true;
    }

    @Override
    public Class<AcknowledgedMessage> getDataItemType() {
        return AcknowledgedMessage.class;
    }

}
