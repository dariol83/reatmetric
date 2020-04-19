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

package eu.dariolucia.reatmetric.ui.mimics.impl;

import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.ui.mimics.SvgAttributeProcessor;
import org.w3c.dom.Element;

abstract public class AttributeSetterProcessor extends SvgAttributeProcessor {

    public AttributeSetterProcessor(Element element, String name, String value) {
        super(element, name, value);
    }

    @Override
    public Runnable buildUpdate(ParameterData parameterData) {
        String valueToApply = expression.apply(parameterData);
        // If value is null, then the attribute is removed
        return new AttributeValueApplier(element, getAttributeToChange(), valueToApply);
    }

    protected abstract String getAttributeToChange();

    private static class AttributeValueApplier implements Runnable {

        private final Element element;
        private final String value;
        private final String attributeName;

        public AttributeValueApplier(Element element, String attributeName, String value) {
            this.element = element;
            this.value = value;
            this.attributeName = attributeName;
        }

        @Override
        public void run() {
            if(value == null) {
                element.removeAttribute(attributeName);
            } else {
                element.setAttribute(attributeName, value);
            }
        }
    }
}
