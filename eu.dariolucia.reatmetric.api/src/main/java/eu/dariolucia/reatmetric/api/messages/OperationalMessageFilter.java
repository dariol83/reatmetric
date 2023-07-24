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

    public String getMessageTextContains() {
        return messageTextContains;
    }

    public Set<String> getSourceList() {
        return sourceList;
    }

    public Set<String> getIdList() {
        return idList;
    }

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
