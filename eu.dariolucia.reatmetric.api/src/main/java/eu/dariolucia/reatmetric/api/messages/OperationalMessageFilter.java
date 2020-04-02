/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.messages;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.model.SystemEntity;

import java.io.Serializable;
import java.util.*;

/**
 *
 * @author dario
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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.messageTextContains);
        hash = 19 * hash + Objects.hashCode(this.idList);
        hash = 19 * hash + Objects.hashCode(this.sourceList);
        hash = 19 * hash + Objects.hashCode(this.severityList);
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
        final OperationalMessageFilter other = (OperationalMessageFilter) obj;
        if (!Objects.equals(this.messageTextContains, other.messageTextContains)) {
            return false;
        }
        if (!Objects.equals(this.idList, other.idList)) {
            return false;
        }
        if (!Objects.equals(this.sourceList, other.sourceList)) {
            return false;
        }
        if (!Objects.equals(this.severityList, other.severityList)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "OperationalMessageFilter [" + "messageRegExp=" + messageTextContains + ", sourceList=" + sourceList + ", severityList=" + severityList + ']';
    }

}
