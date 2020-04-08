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

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import org.w3c.dom.Element;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

abstract public class SvgAttributeProcessor implements Comparable<SvgAttributeProcessor>, Predicate<ParameterData> {

    private static final Logger LOG = Logger.getLogger(SvgAttributeProcessor.class.getName());

    protected final Function<ParameterData, Object> ENG_VALUE_EXTRACTOR = ParameterData::getEngValue;
    protected final Function<ParameterData, Object> RAW_VALUE_EXTRACTOR = ParameterData::getSourceValue;
    protected final Function<ParameterData, Object> VALIDITY_EXTRACTOR = ParameterData::getValidity;
    protected final Function<ParameterData, Object> ALARM_EXTRACTOR = ParameterData::getAlarmState;

    protected final Element element;
    protected final String attributeName;

    protected Function<ParameterData, Boolean> condition;
    protected Function<ParameterData, String> expression;

    public SvgAttributeProcessor(Element element, String attributeName, String attributeValue) {
        this.element = element;
        this.attributeName = attributeName;
        // Parse condition expression
        parseConditionExpression(attributeValue);
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

    private void parseCondition(String trim) {
        // A condition is composed by 3 fields, separated by space: <reference>' '<operator>' '<reference value>'
        String[] parts = trim.split(" ", -1);
        Function<ParameterData, Object> referenceExtractor = parseReference(parts[0], true);
        Function<ParameterData, Object> referenceValueExtractor = parseReference(parts[2], false);
        condition = new ConditionEvaluator(referenceExtractor, parts[1], referenceValueExtractor);
    }

    private Function<ParameterData, Object> parseReference(String val, boolean onlyParameterDataRefs) {
        if(val.equals(SvgConstants.REF_VALUE_ENG)) {
            return ENG_VALUE_EXTRACTOR;
        } else if(val.equals(SvgConstants.REF_VALUE_RAW)) {
            return RAW_VALUE_EXTRACTOR;
        } else if(val.equals(SvgConstants.REF_VALUE_VALIDITY)) {
            return VALIDITY_EXTRACTOR;
        } else if(val.equals(SvgConstants.REF_VALUE_ALARM)) {
            return ALARM_EXTRACTOR;
        } else if(onlyParameterDataRefs) {
            throw new IllegalArgumentException("Cannot parse " + val + " as reference");
        } else {
            return new FixedExtractor(val);
        }
    }

    private void parseExpression(String expression) {
        if(expression.equals(SvgConstants.REF_VALUE_ENG)) {
            this.expression = ENG_VALUE_EXTRACTOR.andThen(ValueUtil::toString);
        } else if(expression.equals(SvgConstants.REF_VALUE_RAW)) {
            this.expression = RAW_VALUE_EXTRACTOR.andThen(ValueUtil::toString);
        } else if(expression.equals(SvgConstants.REF_VALUE_VALIDITY)) {
            this.expression = VALIDITY_EXTRACTOR.andThen(ValueUtil::toString);
        } else if(expression.equals(SvgConstants.REF_VALUE_ALARM)) {
            this.expression = ALARM_EXTRACTOR.andThen(ValueUtil::toString);
        } else {
            // Flat string
            this.expression = parameterData -> expression;
        }
    }

    @Override
    public boolean test(ParameterData parameterData) {
        return condition.apply(parameterData);
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

    public abstract Runnable buildUpdate(ParameterData parameterData);

    private static class FixedExtractor implements Function<ParameterData, Object> {

        private final Object value;

        public FixedExtractor(String val) {
            if(val == null || val.equals(SvgConstants.NULL_VALUE)) {
                this.value = null;
            } else {
                this.value = ValueUtil.tryParse(val).getFirst();
            }
        }

        @Override
        public Object apply(ParameterData parameterData) {
            return value;
        }
    }

    private class ConditionEvaluator implements Function<ParameterData, Boolean> {

        private final Function<ParameterData, Object> referenceExtractor;
        private final Function<ParameterData, Object> referenceValueExtractor;
        private final SvgConditionOperator operator;

        public ConditionEvaluator(Function<ParameterData, Object> referenceExtractor, String operator, Function<ParameterData, Object> referenceValueExtractor) {
            this.referenceExtractor = referenceExtractor;
            this.referenceValueExtractor = referenceValueExtractor;
            this.operator = SvgConditionOperator.valueOf(operator);
        }

        @Override
        public Boolean apply(ParameterData parameterData) {
            Object o1 = referenceExtractor.apply(parameterData);
            Object o2 = referenceValueExtractor.apply(parameterData);
            if(o1 instanceof AlarmState) {
                if(o2 != null) {
                    o2 = AlarmState.valueOf(o2.toString());
                }
            }
            if(o1 instanceof Validity) {
                if(o2 != null) {
                    o2 = Validity.valueOf(o2.toString());
                }
            }
            LOG.info("Checking condition for attribute " + attributeName + ", " + o1 + " " + operator + " " + o2 + " - " + parameterData.getPath().asString());
            return operator.apply(o1, o2);
        }
    }
}
