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

import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import eu.dariolucia.reatmetric.scheduler.IInternalResolver;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.*;

/**
 * A state machine that can invoke activities depending on the values of a set of parameters.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BotProcessingDefinition implements Serializable {

    public static final int STATE_NOT_INITED = -1;

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlAttribute(name = "execute-on-init")
    private boolean executeOnInit = false;

    @XmlAttribute(name = "enabled")
    private boolean enabled = true;

    @XmlElement(name="bot-state")
    private List<BotStateDefinition> botStates = new LinkedList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isExecuteOnInit() {
        return executeOnInit;
    }

    public void setExecuteOnInit(boolean executeOnInit) {
        this.executeOnInit = executeOnInit;
    }

    public List<BotStateDefinition> getBotStates() {
        return botStates;
    }

    public void setBotStates(List<BotStateDefinition> botStates) {
        this.botStates = botStates;
    }

    /*
     * Transient objects
     */

    private transient Set<String> monitoredParameters;

    private transient int currentState = STATE_NOT_INITED;

    public Set<String> getMonitoredParameters() {
        if(monitoredParameters == null) {
            monitoredParameters = new TreeSet<>();
            for(BotStateDefinition bsd : botStates) {
                for(MatcherDefinition md : bsd.getConditions()) {
                    monitoredParameters.add(md.getParameter());
                    if(md.getReference() != null) {
                        monitoredParameters.add(md.getReference());
                    }
                }
            }
        }
        return monitoredParameters;
    }

    public boolean isAffectedBy(List<ParameterData> updates) {
        for(ParameterData pd : updates) {
            if(getMonitoredParameters().contains(pd.getPath().asString())) {
                return true;
            }
        }
        return false;
    }

    public List<SchedulingRequest> evaluate(IInternalResolver resolver) {
        if(!enabled) {
            return Collections.emptyList();
        }
        // If inited, first evaluate the current state: if the evaluation of the current state is OK, do nothing
        if(currentState != STATE_NOT_INITED && botStates.get(currentState).evaluate(resolver)) {
            return Collections.emptyList();
        }
        // If not inited or the current state is not OK, evaluate in order, the first positive evaluation is used
        for(int i = 0; i < botStates.size(); ++i) {
            if(botStates.get(i).evaluate(resolver)) {
                if(currentState != STATE_NOT_INITED || executeOnInit) {
                    currentState = i;
                    return botStates.get(i).buildActionRequests(resolver, name);
                } else {
                    currentState = i;
                    return Collections.emptyList();
                }
            }
        }
        // If no positive evaluation is found, stay in the current state
        return Collections.emptyList();
    }
}
