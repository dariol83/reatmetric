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

package eu.dariolucia.reatmetric.scheduler.definition;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import eu.dariolucia.reatmetric.scheduler.IInternalResolver;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BotStateDefinition implements Serializable {

    private static final Logger LOG = Logger.getLogger(BotStateDefinition.class.getName());

    @XmlAttribute(name="name", required = true)
    private String name;

    @XmlElement(name="condition")
    private List<MatcherDefinition> conditions = new LinkedList<>();

    @XmlElement(name="action")
    private List<ActivityInvocationDefinition> actions = new LinkedList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MatcherDefinition> getConditions() {
        return conditions;
    }

    public void setConditions(List<MatcherDefinition> conditions) {
        this.conditions = conditions;
    }

    public List<ActivityInvocationDefinition> getActions() {
        return actions;
    }

    public void setActions(List<ActivityInvocationDefinition> actions) {
        this.actions = actions;
    }

    /*
     * Transient (stateless)
     */

    public boolean evaluate(IInternalResolver resolver, String source) {
        return conditions.stream().allMatch(o -> {
            try {
                return o.execute(resolver);
            } catch (ReatmetricException e) {
                LOG.log(logRecord(Level.WARNING, "Cannot evalute condition in " + getName() + ": " + e.getMessage(), e, new Object[] {source, null}));
                return false;
            }
        });
    }

    public List<SchedulingRequest> buildActionRequests(IInternalResolver resolver, String source) {
        List<SchedulingRequest> toReturn = new LinkedList<>();
        for(ActivityInvocationDefinition aid : actions) {
            SchedulingRequest req = aid.build(resolver, source + "-" + name);
            if(req != null) {
                toReturn.add(req);
            } else {
                LOG.log(logRecord(Level.WARNING, "Cannot build action request in " + getName() + " for activity" + aid.getActivity(), null, new Object[] {source, null}));
            }
        }
        return toReturn;
    }

    private LogRecord logRecord(Level level, String msg, Throwable e, Object[] objects) {
        LogRecord lr = new LogRecord(level, msg);
        lr.setThrown(e);
        lr.setParameters(objects);
        return lr;
    }
}
