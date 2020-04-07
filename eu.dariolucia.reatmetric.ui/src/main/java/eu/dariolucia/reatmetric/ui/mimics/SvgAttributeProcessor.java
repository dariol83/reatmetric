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

package eu.dariolucia.reatmetric.ui.mimics;

import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import org.w3c.dom.Element;

import java.util.Objects;

abstract public class SvgAttributeProcessor implements Comparable<SvgAttributeProcessor> {

    protected final Element element;
    protected final String attributeName;

    public SvgAttributeProcessor(Element element, String attributeName) {
        this.element = element;
        this.attributeName = attributeName;
    }

    @Override
    public int compareTo(SvgAttributeProcessor o) {
        return this.attributeName.compareTo(o.attributeName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SvgAttributeProcessor that = (SvgAttributeProcessor) o;
        return Objects.equals(attributeName, that.attributeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeName);
    }

    public abstract boolean apply(ParameterData parameterData);

    public abstract Runnable buildUpdate(ParameterData parameterData);

}
