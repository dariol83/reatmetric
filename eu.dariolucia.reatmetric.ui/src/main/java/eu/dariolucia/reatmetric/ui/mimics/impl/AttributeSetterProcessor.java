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
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import static eu.dariolucia.reatmetric.ui.mimics.SvgConstants.*;
import static eu.dariolucia.reatmetric.ui.mimics.SvgConstants.BLINK_PREFIX;

abstract public class AttributeSetterProcessor extends SvgAttributeProcessor {

    public AttributeSetterProcessor(Element element, String name, String value) {
        super(element, name, value);
    }

    protected volatile Attr attributeToSet;

    @Override
    public Runnable buildUpdate(ParameterData parameterData) {
        String valueToApply = expression.apply(parameterData);
        if(valueToApply == null) {
            valueToApply = "";
        }
        if(attributeToSet == null) {
            findAttributeToSet(getAttributeToChange());
        }
        return new AttributeValueApplier(element, attributeToSet, valueToApply);
    }

    private void findAttributeToSet(String attributeToChange) {
        NamedNodeMap attributesMap = element.getAttributes();
        if(attributesMap != null) {
            for(int i = 0; i < attributesMap.getLength(); ++i) {
                Attr attribute = (Attr) attributesMap.item(i);
                if(attribute.getName().equals(attributeToChange)) {
                    attributeToSet = attribute;
                    return;
                }
            }
        }
        // If the attribute is not there, then it will be created
    }

    protected abstract String getAttributeToChange();

    private static class AttributeValueApplier implements Runnable {

        private final Element element;
        private final Attr attribute;
        private final String value;

        public AttributeValueApplier(Element element, Attr attribute, String value) {
            this.element = element;
            this.attribute = attribute;
            this.value = value;
        }

        @Override
        public void run() {
            if(attribute != null) {
                attribute.setValue(value);
            } else {

            }
        }
    }

}
