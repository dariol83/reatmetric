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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "scheduler", namespace = "http://dariolucia.eu/reatmetric/scheduler")
@XmlAccessorType(XmlAccessType.FIELD)
public class SchedulerConfiguration {

    public static SchedulerConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(SchedulerConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            SchedulerConfiguration o = (SchedulerConfiguration) u.unmarshal(is);
            return o;
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    @XmlAttribute(name = "scheduler-enabled")
    private boolean schedulerEnabled = true;

    @XmlElement(name = "bot-definition")
    private List<BotProcessingDefinition> bots = new LinkedList<>();

    public List<BotProcessingDefinition> getBots() {
        return bots;
    }

    public void setBots(List<BotProcessingDefinition> bots) {
        this.bots = bots;
    }

    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }

    public void setSchedulerEnabled(boolean schedulerEnabled) {
        this.schedulerEnabled = schedulerEnabled;
    }
}
