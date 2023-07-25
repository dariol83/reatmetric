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
import java.util.*;

/**
 * This class allows to filter/subscribe/retrieve operational messages.
 *
 * Objects of this class are immutable.
 */
public final class OperationalMessageFilter extends AbstractDataItemFilter<OperationalMessage> implements Serializable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String messageTextContains;

	private final Set<String> idList;

    private final Set<String> sourceList;
    
    private final Set<Severity> severityList;

    /**
     * The constructor of the operational message filter.
     *
     * @param messageTextContains the text that must be contained in the message. It can be null.
     * @param idList the message ids to select. It can be null: if so, all ids are selected.
     * @param sourceList the sources to select. It can be null: if so, all sources are selected.
     * @param severityList the severities to select. It can be null: if so, all severities are selected.
     */
    public OperationalMessageFilter(String messageTextContains, Collection<String> idList, Collection<String> sourceList, Collection<Severity> severityList) {
        this.messageTextContains = messageTextContains;
        if(idList != null) {
            this.idList = Collections.unmodifiableSet(new LinkedHashSet<>(idList));
        } else {
            this.idList = null;
        }
        if(sourceList != null) {
            this.sourceList = Collections.unmodifiableSet(new LinkedHashSet<>(sourceList));
        } else {
            this.sourceList = null;
        }
        if(severityList != null) {
            this.severityList = Collections.unmodifiableSet(new LinkedHashSet<>(severityList));
        } else {
            this.severityList = null;
        }
    }

    /**
     * The text that must be contained in the message.
     *
     * It can be null.
     *
     * @return the text
     */
    public String getMessageTextContains() {
        return messageTextContains;
    }

    /**
     * The set of sources to select: an operational message is selected if its source is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified sources
     */
    public Set<String> getSourceList() {
        return sourceList;
    }

    /**
     * The set of IDs to select: an operational message is selected if its ID is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified IDs
     */
    public Set<String> getIdList() {
        return idList;
    }

    /**
     * The set of severities to select: an operational message is selected if its severity is one of those
     * specified in the filter.
     *
     * It can be null.
     *
     * @return the specified severity
     */
    public Set<Severity> getSeverityList() {
        return severityList;
    }
    
    @Override
    public boolean isClear() {
        return this.messageTextContains == null && this.idList == null && this.severityList == null && this.sourceList == null;
    }

    @Override
    public boolean test(OperationalMessage item) {
        if(messageTextContains != null && !item.getMessage().contains(messageTextContains)) {
            return false;
        }
        if(idList != null && !idList.contains(item.getId())) {
            return false;
        }
        if(severityList != null && !severityList.contains(item.getSeverity())) {
            return false;
        }
        if(sourceList != null && !sourceList.contains(item.getSource())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean select(SystemEntity entity) {
        return true;
    }

    @Override
    public Class<OperationalMessage> getDataItemType() {
        return OperationalMessage.class;
    }

}
