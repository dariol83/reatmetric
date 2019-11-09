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
	private static final long serialVersionUID = 1626449016479916750L;

	private final String messageRegExp;
    
    private final List<String> sourceList;
    
    private final List<Severity> severityList;

    public OperationalMessageFilter(String messageRegExp, List<String> sourceList, List<Severity> severityList) {
        this.messageRegExp = messageRegExp;
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

    public String getMessageRegExp() {
        return messageRegExp;
    }

    public List<String> getSourceList() {
        if(sourceList == null) {
            return null;
        }
        return new ArrayList<>(sourceList);
    }

    public List<Severity> getSeverityList() {
        if(severityList == null) {
            return null;
        }
        return new ArrayList<>(severityList);
    }
    
    @Override
    public boolean isClear() {
        return this.messageRegExp == null && this.severityList == null && this.sourceList == null;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.messageRegExp);
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
        if (!Objects.equals(this.messageRegExp, other.messageRegExp)) {
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
        return "OperationalMessageFilter [" + "messageRegExp=" + messageRegExp + ", sourceList=" + sourceList + ", severityList=" + severityList + ']';
    }

}
