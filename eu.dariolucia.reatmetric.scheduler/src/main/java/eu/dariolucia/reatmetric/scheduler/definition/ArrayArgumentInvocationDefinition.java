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

import eu.dariolucia.reatmetric.api.processing.input.AbstractActivityArgument;
import eu.dariolucia.reatmetric.api.processing.input.ArrayActivityArgument;
import eu.dariolucia.reatmetric.api.processing.input.ArrayActivityArgumentRecord;
import eu.dariolucia.reatmetric.api.processing.input.PlainActivityArgument;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ArrayArgumentInvocationDefinition extends AbstractArgumentInvocationDefinition implements Serializable {

    @XmlElement(name = "records")
    private List<ArrayArgumentRecordInvocationDefinition> records = new LinkedList<>();

    public List<ArrayArgumentRecordInvocationDefinition> getRecords() {
        return records;
    }

    public void setRecords(List<ArrayArgumentRecordInvocationDefinition> records) {
        this.records = records;
    }

    @Override
    public AbstractActivityArgument build() {
        List<ArrayActivityArgumentRecord> records = new LinkedList<>();
        for(ArrayArgumentRecordInvocationDefinition ageid : getRecords()) {
            List<AbstractActivityArgument> argsForGroupRecord = new LinkedList<>();
            for(AbstractArgumentInvocationDefinition aaid : ageid.getElements()) {
                argsForGroupRecord.add(aaid.build());
            }
            ArrayActivityArgumentRecord elem = new ArrayActivityArgumentRecord(argsForGroupRecord);
            records.add(elem);
        }
        return new ArrayActivityArgument(getName(), records);
    }
}
