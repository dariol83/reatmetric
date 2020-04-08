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

public class TextSetterProcessor extends SvgAttributeProcessor {

    public TextSetterProcessor(Element element, String name, String value) {
        super(element, name, value);
    }

    @Override
    public Runnable buildUpdate(ParameterData parameterData) {
        String valueToApply = expression.apply(parameterData);
        return new TextValueApplier(element, valueToApply);
    }

    private static class TextValueApplier implements Runnable {

        private final Element element;
        private final String value;

        public TextValueApplier(Element element, String value) {
            this.element = element;
            this.value = value;
        }

        @Override
        public void run() {
            element.setTextContent(value);
        }
    }

}
