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

import java.util.function.Function;

public class FillAttributeProcessor extends SvgAttributeProcessor {

    protected Function<ParameterData, Boolean> condition;
    protected Function<ParameterData, String> expression;
    protected String attributeToChange = "fill";

    public FillAttributeProcessor(Element element, String name, String conditionExpression) {
        super(element, name);
        // Parse condition expression
        parseConditionExpression(conditionExpression);
    }

    private void parseConditionExpression(String conditionExpression) {
        conditionExpression = conditionExpression.trim();
        if(conditionExpression.startsWith(":=")) {
            // Condition -> true
            condition = pd -> true;
            // Expression to parse
            parseExpression(conditionExpression.substring(2).trim());
        } else {
            int idx = conditionExpression.indexOf(":=");
            parseCondition(conditionExpression.substring(0, idx).trim());
            parseExpression(conditionExpression.substring(idx + 2).trim());
        }
    }

    @Override
    public boolean apply(ParameterData parameterData) {
        return condition.apply(parameterData);
    }

    @Override
    public Runnable buildUpdate(ParameterData parameterData) {
        String valueToApply = expression.apply(parameterData);
        return new AttributeValueApplier(element, attributeToChange, valueToApply);
    }

    private static class AttributeValueApplier implements Runnable {

        private final Element element;
        private final String attribute;
        private final String value;

        public AttributeValueApplier(Element element, String attribute, String value) {
            this.element = element;
            this.attribute = attribute;
            this.value = value;
        }

        @Override
        public void run() {
            element.setAttribute(attribute, value);
        }
    }
}
