/*
 * Copyright (c)  2026 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.processing.impl.graph.GraphModel;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ProcessingModelValidatorTest {

    @Test
    void testValidationCollectsAllRequestedIssueTypes() {
        ProcessingDefinition definition = buildInvalidDefinition();

        List<ProcessingModelValidationIssue> issues = definition.validate();
        List<ProcessingModelValidationIssueType> issueTypes = issues.stream()
                .map(ProcessingModelValidationIssue::getType)
                .collect(Collectors.toList());

        assertTrue(issueTypes.contains(ProcessingModelValidationIssueType.DUPLICATE_ENTITY_ID));
        assertTrue(issueTypes.contains(ProcessingModelValidationIssueType.DUPLICATE_ENTITY_PATH));
        assertTrue(issueTypes.contains(ProcessingModelValidationIssueType.INVALID_XY_CALIBRATION_POINTS));
        assertTrue(issueTypes.contains(ProcessingModelValidationIssueType.UNPARSEABLE_DEFAULT_VALUE));
        assertTrue(issueTypes.contains(ProcessingModelValidationIssueType.NULL_DEFAULT_VALUE));
        assertTrue(issueTypes.contains(ProcessingModelValidationIssueType.MISSING_REFERENCED_ENTITY));
        assertTrue(issueTypes.contains(ProcessingModelValidationIssueType.GROOVY_EXPRESSION_SYNTAX_ERROR));
        assertTrue(issueTypes.contains(ProcessingModelValidationIssueType.DUPLICATE_ACTIVITY_ARGUMENT_NAME));
    }

    @Test
    void testGraphBuildFailsWithConsolidatedValidationMessage() {
        ProcessingDefinition definition = buildInvalidDefinition();

        ProcessingModelException exception = assertThrows(ProcessingModelException.class, () -> new GraphModel(definition, null).build());

        String message = exception.getMessage();
        assertTrue(message.contains(ProcessingModelValidationIssueType.DUPLICATE_ENTITY_ID.name()));
        assertTrue(message.contains(ProcessingModelValidationIssueType.GROOVY_EXPRESSION_SYNTAX_ERROR.name()));
        assertTrue(message.lines().count() > 1);
    }

    private ProcessingDefinition buildInvalidDefinition() {
        ParameterProcessingDefinition firstParameter = parameter(1, "ROOT.PARAM");
        firstParameter.setDefaultValue(new FixedDefaultValue(DefaultValueType.RAW, "not-an-integer"));
        firstParameter.setCalibrations(Collections.singletonList(new XYCalibration(Collections.singletonList(new XYCalibrationPoint(1, 1)), false)));
        firstParameter.setExpression(new ExpressionDefinition("if (", Collections.singletonList(new SymbolDefinition("missing", 999, PropertyBinding.ENG_VALUE))));

        ParameterProcessingDefinition duplicateParameter = parameter(1, "ROOT.PARAM");

        ActivityProcessingDefinition activity = new ActivityProcessingDefinition();
        activity.setId(10);
        activity.setLocation("ROOT.ACTIVITY");
        activity.setType("TC");
        PlainArgumentDefinition firstArgument = argument("ARG", new FixedDefaultValue(DefaultValueType.RAW, null));
        PlainArgumentDefinition duplicateArgument = argument("ARG", new ReferenceDefaultValue(DefaultValueType.RAW, parameter(99, "ROOT.MISSING"), DefaultValueType.RAW));
        activity.setArguments(Arrays.asList(firstArgument, duplicateArgument));

        return new ProcessingDefinition(Arrays.asList(firstParameter, duplicateParameter), Collections.emptyList(), Collections.singletonList(activity));
    }

    private ParameterProcessingDefinition parameter(int id, String location) {
        ParameterProcessingDefinition parameter = new ParameterProcessingDefinition();
        parameter.setId(id);
        parameter.setLocation(location);
        parameter.setRawType(ValueTypeEnum.SIGNED_INTEGER);
        parameter.setEngineeringType(ValueTypeEnum.SIGNED_INTEGER);
        return parameter;
    }

    private PlainArgumentDefinition argument(String name, AbstractDefaultValue defaultValue) {
        return new PlainArgumentDefinition(name, ValueTypeEnum.SIGNED_INTEGER, ValueTypeEnum.SIGNED_INTEGER, "", false, defaultValue, null, Collections.emptyList());
    }
}
