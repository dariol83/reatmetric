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

package eu.dariolucia.reatmetric.driver.spacecraft.definition.services;

import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import java.io.IOException;
import java.io.InputStream;

@XmlRootElement(name = "onboard-scheduling-service", namespace = "http://dariolucia.eu/reatmetric/driver/spacecraft/onboard-scheduling-service")
@XmlAccessorType(XmlAccessType.FIELD)
public class OnboardOperationsSchedulingServiceConfiguration {

    public static OnboardOperationsSchedulingServiceConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(OnboardOperationsSchedulingServiceConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            OnboardOperationsSchedulingServiceConfiguration o = (OnboardOperationsSchedulingServiceConfiguration) u.unmarshal(is);
            return o;
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    @XmlAttribute(name = "schedule-activity-path", required=true)
    private String activityPath;

    @XmlAttribute(name = "sub-schedule-id-name")
    private String subscheduleIdName = null;

    @XmlAttribute(name = "num-commands-name")
    private String numCommandsName = null;

    @XmlAttribute(name = "array-used")
    private boolean arrayUsed = false;

    public String getActivityPath() {
        return activityPath;
    }

    public void setActivityPath(String activityPath) {
        this.activityPath = activityPath;
    }

    public String getSubscheduleIdName() {
        return subscheduleIdName;
    }

    public void setSubscheduleIdName(String subscheduleIdName) {
        this.subscheduleIdName = subscheduleIdName;
    }

    public boolean isArrayUsed() {
        return arrayUsed;
    }

    public void setArrayUsed(boolean arrayUsed) {
        this.arrayUsed = arrayUsed;
    }

    public String getNumCommandsName() {
        return numCommandsName;
    }

    public void setNumCommandsName(String numCommandsName) {
        this.numCommandsName = numCommandsName;
    }
}
