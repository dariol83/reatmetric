/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.LinkedList;
import java.util.List;

/**
 * An activity is an operation that can be requested through a route. An activity request spawns
 * an activity occurrence, whose lifecycle is tracked by the following states:
 * <ul>
 *     <li>Release: completed when the request leaves the connector identified by the route</li>
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
public class ActivityProcessingDefinition extends AbstractProcessingDefinition {

    @XmlAttribute(name = "type", required = true)
    private String type;

    @XmlAttribute(name = "defaultRoute")
    private String defaultRoute;

    @XmlAttribute(name = "transmissionTimeout")
    private int transmissionTimeout = 0; // in seconds

    @XmlAttribute(name = "executionTimeout")
    private int executionTimeout = 0; // in seconds

    @XmlAttribute(name = "verificationTimeout")
    private int verificationTimeout = 5; // in seconds

    @XmlElement(name = "argument")
    private List<ArgumentDefinition> arguments = new LinkedList<>();

    @XmlElement(name = "property")
    private List<KeyValue> properties = new LinkedList<>();

    @XmlElement(name = "verification")
    private ExpressionDefinition verification;

    public ActivityProcessingDefinition() {
        super();
    }

    public ActivityProcessingDefinition(int id, String description, String location, String type, String defaultRoute, int transmissionTimeout, int executionTimeout, int verificationTimeout, List<ArgumentDefinition> arguments, List<KeyValue> properties, ExpressionDefinition verification) {
        super(id, description, location);
        this.type = type;
        this.defaultRoute = defaultRoute;
        this.transmissionTimeout = transmissionTimeout;
        this.executionTimeout = executionTimeout;
        this.verificationTimeout = verificationTimeout;
        this.arguments = arguments;
        this.properties = properties;
        this.verification = verification;
    }

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

    public List<ArgumentDefinition> getArguments() {
        return arguments;
    }

    public void setArguments(List<ArgumentDefinition> arguments) {
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
}
