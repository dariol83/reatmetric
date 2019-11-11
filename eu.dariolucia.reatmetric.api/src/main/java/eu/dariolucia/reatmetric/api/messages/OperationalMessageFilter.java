/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.messages;

import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author dario
 */
public final class OperationalMessageFilter extends AbstractDataItemFilter implements Serializable {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String messageTextContains;

	private final List<String> idList;

    private final List<String> sourceList;
    
    private final List<Severity> severityList;

    public OperationalMessageFilter(String messageTextContains, List<String> idList, List<String> sourceList, List<Severity> severityList) {
        this.messageTextContains = messageTextContains;
        if(idList != null) {
            this.idList = new ArrayList<>(idList);
        } else {
            this.idList = null;
        }
        if(sourceList != null) {
            this.sourceList = new ArrayList<>(sourceList);
        } else {
            this.sourceList = null;
        }
        if(severityList != null) {
            this.severityList = new ArrayList<>(severityList);
        } else {
            this.severityList = null;
        }
    }

    public String getMessageTextContains() {
        return messageTextContains;
    }

    public List<String> getSourceList() {
        if(sourceList == null) {
            return null;
        }
        return new ArrayList<>(sourceList);
    }

    public List<String> getIdList() {
        if(idList == null) {
            return null;
        }
        return new ArrayList<>(idList);
    }

    public List<Severity> getSeverityList() {
        if(severityList == null) {
            return null;
        }
        return new ArrayList<>(severityList);
    }
    
    @Override
    public boolean isClear() {
        return this.messageTextContains == null && this.idList == null && this.severityList == null && this.sourceList == null;
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
