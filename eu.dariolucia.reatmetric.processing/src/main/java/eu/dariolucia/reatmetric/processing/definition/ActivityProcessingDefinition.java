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
 * An activity is an operation that can be requested to a controlled system through a route. An activity request spawns
 * an activity occurrence, whose lifecycle is tracked by the following states:
 * <ul>
 *     <li>Release</li>
 *     <li>Transmission</li>
 *     <li>Scheduled</li>
 *     <li>Execution</li>
 *     <li>Verification</li>
 * </ul>
 * Each state is reported as a set of activity updates and it is the responsibility of the activity handler to announce
 * the transition to a new state.
 * For the transmission, execution and verification states, an optional timeout can be specified: when the timeout elapses,
 * the activity occurrence is marked as timed out and further updates are discarded.
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
    private int transmissionTimeout = 10; // in seconds

    @XmlAttribute(name = "executionTimeout")
    private int executionTimeout = 3600; // in seconds

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

}
