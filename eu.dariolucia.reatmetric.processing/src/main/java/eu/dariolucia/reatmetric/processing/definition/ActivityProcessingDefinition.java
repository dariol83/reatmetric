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

package eu.dariolucia.reatmetric.processing.definition;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * An activity is an operation that can be requested through a route. An activity request spawns
 * an activity occurrence, whose lifecycle is tracked by the following states:
 * <ul>
 *     <li>Release: completed when the request leaves the sle identified by the route</li>
 *     <li>Transmission: completed when the request reaches the target system</li>
 *     <li>Scheduled: if the activity occurrence is remotely scheduled</li>
 *     <li>Execution: completed when the request has been executed by the target system</li>
 *     <li>Verification: completed when the result of the execution has been confirmed by telemetry parameter</li>
 * </ul>
 * Each state is reported as a set of activity updates and it is the responsibility of the activity handler to announce
 * the transition to a new state.
 * For the transmission, execution and verification states, an optional timeout can be specified: when the timeout elapses,
 * the activity occurrence is marked as timed out: further updates are still processed.
 * An activity is also marked with a type: an activity handler must report the list of supported types. The activity
 * can be forwarded to an handler only if the selected handler supports the activity type.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ActivityProcessingDefinition extends AbstractProcessingDefinition implements Serializable {

    @XmlAttribute(name = "type", required = true)
    private String type;

    @XmlAttribute(name = "defaultRoute")
    private String defaultRoute;

    @XmlAttribute(name = "transmission_timeout")
    private int transmissionTimeout = 0; // in seconds, 0 means disabled

    @XmlAttribute(name = "execution_timeout")
    private int executionTimeout = 0; // in seconds, 0 means disabled

    @XmlAttribute(name = "verification_timeout")
    private int verificationTimeout = 0; // in seconds, 0 means disabled

    @XmlAttribute(name = "expected_duration")
    private int expectedDuration = 1000; // in milliseconds, only positive numbers allowed, default: 1 second

    @XmlElements({
            @XmlElement(name="argument",type= PlainArgumentDefinition.class),
            @XmlElement(name="array",type= ArrayArgumentDefinition.class)
    })
    private List<AbstractArgumentDefinition> arguments = new LinkedList<>();

    @XmlElement(name = "property")
    private List<KeyValue> properties = new LinkedList<>();

    @XmlElement(name = "verification")
    private ExpressionDefinition verification;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultRoute() {
        return defaultRoute;
    }

    public void setDefaultRoute(String defaultRoute) {
        this.defaultRoute = defaultRoute;
    }

    public int getTransmissionTimeout() {
        return transmissionTimeout;
    }

    public void setTransmissionTimeout(int transmissionTimeout) {
        this.transmissionTimeout = transmissionTimeout;
    }

    public int getExecutionTimeout() {
        return executionTimeout;
    }

    public void setExecutionTimeout(int executionTimeout) {
        this.executionTimeout = executionTimeout;
    }

    public int getVerificationTimeout() {
        return verificationTimeout;
    }

    public void setVerificationTimeout(int verificationTimeout) {
        this.verificationTimeout = verificationTimeout;
    }

    public List<AbstractArgumentDefinition> getArguments() {
        return arguments;
    }

    public void setArguments(List<AbstractArgumentDefinition> arguments) {
        this.arguments = arguments;
    }

    public List<KeyValue> getProperties() {
        return properties;
    }

    public void setProperties(List<KeyValue> properties) {
        this.properties = properties;
    }

    public ExpressionDefinition getVerification() {
        return verification;
    }

    public void setVerification(ExpressionDefinition verification) {
        this.verification = verification;
    }

    public AbstractArgumentDefinition getArgumentByName(String argument) {
        return this.arguments.stream().filter(o -> o.getName().equals(argument)).findFirst().orElse(null);
    }

    public int getExpectedDuration() {
        return expectedDuration;
    }

    public void setExpectedDuration(int expectedDuration) {
        this.expectedDuration = expectedDuration;
    }
}
