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
import eu.dariolucia.reatmetric.api.scheduler.BotStateData;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import eu.dariolucia.reatmetric.scheduler.IInternalResolver;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.*;

/**
 * A state machine that can invoke activities depending on the values of a set of parameters.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BotProcessingDefinition implements Serializable {

    public static final int STATE_NOT_INITED = -1;
    public static final String NOT_INITIALISED_STATE_NAME = "<not initialised>";

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

    private transient BotStateData currentBotState = null;

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
        if(currentState != STATE_NOT_INITED && botStates.get(currentState).evaluate(resolver, getName())) {
            return Collections.emptyList();
        }
        // If not inited or the current state is not OK, evaluate in order, the first positive evaluation is used
        for(int i = 0; i < botStates.size(); ++i) {
            if(botStates.get(i).evaluate(resolver, getName())) {
                if(currentState != STATE_NOT_INITED || executeOnInit) {
                    currentState = i;
                    currentBotState = buildStateData();
                    return botStates.get(i).buildActionRequests(resolver, name);
                } else {
                    currentState = i;
                    currentBotState = buildStateData();
                    return Collections.emptyList();
                }
            }
        }
        // If no positive evaluation is found, stay in the current state
        return Collections.emptyList();
    }

    public BotStateData updateEnablement(boolean status) {
        if(status == this.enabled) {
            // Do nothing
            return null;
        }
        if(this.enabled) {
            // Status to be disabled
            this.enabled = false;
        } else {
            // Status to be enabled
            this.enabled = true;
            // Reset the state
            this.currentState = STATE_NOT_INITED;
        }
        currentBotState = buildStateData();
        return currentBotState;
    }

    private String getStateName() {
        if(currentState == STATE_NOT_INITED) {
            return NOT_INITIALISED_STATE_NAME;
        } else {
            return botStates.get(currentState).getName();
        }
    }

    private BotStateData buildStateData() {
        return new BotStateData(getName(),currentState,getStateName(),isEnabled());
    }

    public BotStateData getCurrentState() {
        if(currentBotState == null) {
            currentBotState = new BotStateData(getName(), STATE_NOT_INITED, NOT_INITIALISED_STATE_NAME, isEnabled());
        }
        return currentBotState;
    }
}
