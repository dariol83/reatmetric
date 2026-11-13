/*
 * Copyright (c)  2026 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProcessingDefinitionTest {

    private static final String UNICODE_TEXT = "München & <東京> \"café\" 🚀";
    private static final String SYNTHETIC_EXPRESSION = "SOURCE.value() < 0 ? 'été & <東京>' : 'nominal'";

    @Test
    void preservesDefaultsWhenOptionalXmlAttributesAreOmitted() throws Exception {
        String xml = """
                <p:processing xmlns:p="http://dariolucia.eu/reatmetric/processing/definition">
                    <parameters>
                        <parameter id="#1" location="ROOT.PARAM" raw_type="SIGNED_INTEGER" eng_type="REAL"/>
                    </parameters>
                    <events><event id="#2" location="ROOT.EVENT"/></events>
                    <activities>
                        <activity id="#3" location="ROOT.ACTIVITY" type="TC">
                            <argument name="VALUE" raw_type="SIGNED_INTEGER" eng_type="REAL"/>
                            <verification>
                                <expression>source.value() == 1</expression>
                                <symbol name="source" reference="#1"/>
                            </verification>
                        </activity>
                    </activities>
                </p:processing>
                """;
        ProcessingDefinition loaded = ProcessingDefinition.load(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertUsefulDefaults(loaded);
        assertUsefulDefaults(roundTrip(loaded));
    }

    @Test
    void roundTripsEntityIdentityAndAdaptedIdsAcrossForwardAndBackwardReferences() throws Exception {
        byte[] xml = save(referencedDefinition());
        ProcessingDefinition loaded = ProcessingDefinition.load(new ByteArrayInputStream(xml));
        ParameterProcessingDefinition parameter = parameter(loaded, Integer.MAX_VALUE);
        ActivityProcessingDefinition activity = activity(loaded, 300);
        EventProcessingDefinition event = event(loaded, 200);

        assertEquals(2, loaded.getParameterDefinitions().size());
        assertEquals(1, loaded.getEventDefinitions().size());
        assertEquals(1, loaded.getActivityDefinitions().size());
        assertFalse(loaded.isSyntheticParameterProcessingEnabled());
        assertSame(activity, parameter.getSetter().getActivity());
        assertEquals("TARGET", parameter.getSetter().getSetArgument());
        assertSame(event, parameter.getTriggers().get(0).getEvent());
        assertEquals(TriggerCondition.ON_ALARM_RAISED, parameter.getTriggers().get(0).getTriggerCondition());
        ReferenceDefaultValue reference = assertInstanceOf(ReferenceDefaultValue.class,
                assertInstanceOf(PlainArgumentDefinition.class, activity.getArgumentByName("TARGET")).getDefaultValue());
        assertSame(parameter, reference.getParameter());
        assertEquals(DefaultValueType.ENGINEERING, reference.getType());
        assertEquals(DefaultValueType.ENGINEERING, reference.getTargetValueType());
        assertEquals(Integer.MAX_VALUE, parameter(loaded, 42).getExpression().getSymbols().get(0).getReference());

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml));
        assertEquals("#2147483647", element(document, "parameter").getAttribute("id"));
        assertEquals("#300", element(document, "setter").getAttribute("activity"));
        assertEquals("#200", element(document, "trigger").getAttribute("event"));
        assertEquals("#2147483647", element(document, "default_ref").getAttribute("parameter"));
        assertEquals("#2147483647", element(document, "symbol").getAttribute("reference"));
    }

    @Test
    void roundTripsUnicodeExpressionsAndExecutableCalibrationAndCheckDefinitions() throws Exception {
        ProcessingDefinition loaded = roundTrip(referencedDefinition());
        ParameterProcessingDefinition parameter = parameter(loaded, Integer.MAX_VALUE);
        ActivityProcessingDefinition activity = activity(loaded, 300);

        assertEquals(UNICODE_TEXT, parameter.getDescription());
        assertEquals("µA & <nominal>", parameter.getUnit());
        assertEquals("42", parameter.getDefaultValue().getValue());
        assertEquals(DefaultValueType.RAW, parameter.getDefaultValue().getType());
        assertEquals(SYNTHETIC_EXPRESSION, parameter(loaded, 42).getExpression().getExpression());
        assertEquals(ExpressionDialect.GROOVY, parameter(loaded, 42).getExpression().getDialect());
        FixedDefaultValue label = assertInstanceOf(FixedDefaultValue.class,
                assertInstanceOf(PlainArgumentDefinition.class, activity.getArgumentByName("LABEL")).getDefaultValue());
        assertEquals(UNICODE_TEXT, label.getValue());
        assertEquals("display", activity.getProperties().get(0).getKey());
        assertEquals(UNICODE_TEXT, activity.getProperties().get(0).getValue());
        assertEquals("VALUE > 0 && VALUE < 100", activity.getVerification().getExpression());
        assertEquals(Integer.MAX_VALUE, activity.getVerification().getSymbols().get(0).getReference());

        ExpressionCalibration calibration = assertInstanceOf(ExpressionCalibration.class, parameter.getCalibrations().get(0));
        assertEquals("input * 2 + 0.5", calibration.getDefinition().getExpression());
        assertEquals(84.5, ((Number) calibration.calibrate(42L, null, ValueTypeEnum.REAL)).doubleValue());
        ExpressionCheck check = assertInstanceOf(ExpressionCheck.class, parameter.getChecks().get(0));
        assertEquals("input < 0", check.getDefinition().getExpression());
        assertEquals(CheckSeverity.WARNING, check.getSeverity());
        assertEquals(2, check.getNumViolations());
        assertEquals(AlarmState.NOMINAL, check.check(42L, Instant.EPOCH, 0, null));
        assertEquals(AlarmState.WARNING, check.check(-1L, Instant.EPOCH, 1, null));
    }

    @Test
    void roundTripsTheExistingParameterFixtureIncludingPolymorphicCalibrationsAndMatcherReferences() throws Exception {
        ProcessingDefinition original = fixture("processing_definitions_parameters.xml");
        ProcessingDefinition loaded = roundTrip(original);

        assertEquals(original.getParameterDefinitions().size(), loaded.getParameterDefinitions().size());
        assertEquals(ValueTypeEnum.ABSOLUTE_TIME, parameter(loaded, 109).getRawType());
        PolyCalibration polynomial = assertInstanceOf(PolyCalibration.class, parameter(loaded, 1002).getCalibrations().get(0));
        assertEquals(10.0, polynomial.getA0());
        assertEquals(2.0, polynomial.getA1());
        ExpressionCalibration expression = assertInstanceOf(ExpressionCalibration.class,
                parameter(loaded, 1003).getCalibrations().get(0));
        assertEquals("input + BATTERY_TENSION.value()/300.0", expression.getDefinition().getExpression());
        assertEquals(1002, expression.getDefinition().getSymbols().get(0).getReference());
        EnumCalibration enumeration = assertInstanceOf(EnumCalibration.class, parameter(loaded, 1004).getCalibrations().get(0));
        assertEquals("OFF", enumeration.calibrate(0L, null, ValueTypeEnum.CHARACTER_STRING));
        assertEquals("UNKNOWN", enumeration.calibrate(99L, null, ValueTypeEnum.CHARACTER_STRING));
        ExpectedCheck expected = assertInstanceOf(ExpectedCheck.class, parameter(loaded, 1004).getChecks().get(0));
        assertEquals(List.of("ON"), expected.getExpectedValues());
        assertInstanceOf(LogCalibration.class, parameter(loaded, 1041).getCalibrations().get(0));
        XYCalibration xy = assertInstanceOf(XYCalibration.class, parameter(loaded, 1042).getCalibrations().get(0));
        assertEquals(25.0, ((Number) xy.calibrate(16.25, null, ValueTypeEnum.REAL)).doubleValue());
        MatcherDefinition matcher = parameter(loaded, 1061).getCalibrations().get(0).getApplicability().getMatch();
        assertSame(parameter(loaded, 1065), matcher.getParameter());
        assertSame(parameter(loaded, 1066), matcher.getReference());
        assertEquals(MatcherType.LT, matcher.getOperator());
    }

    @Test
    void roundTripsTheExistingEventFixtureAndParameterTriggerTargets() throws Exception {
        ProcessingDefinition original = fixture("processing_definitions_events.xml");
        ProcessingDefinition loaded = roundTrip(original);

        assertEquals(original.getEventDefinitions().size(), loaded.getEventDefinitions().size());
        assertEquals(Severity.ALARM, event(loaded, 2002).getSeverity());
        assertEquals(3000, event(loaded, 2002).getInhibitionPeriod());
        List<ParameterTriggerDefinition> triggers = parameter(loaded, 1001).getTriggers();
        assertEquals(4, triggers.size());
        assertSame(event(loaded, 2003), triggers.get(0).getEvent());
        assertEquals(TriggerCondition.ON_NEW_SAMPLE, triggers.get(0).getTriggerCondition());
        assertSame(event(loaded, 2006), triggers.get(3).getEvent());
        assertEquals(TriggerCondition.ON_BACK_TO_NOMINAL, triggers.get(3).getTriggerCondition());
        assertEquals("PARAM1 == 20", event(loaded, 2007).getCondition().getExpression());
        SymbolDefinition symbol = event(loaded, 2007).getCondition().getSymbols().get(0);
        assertEquals(1001, symbol.getReference());
        assertEquals(PropertyBinding.ENG_VALUE, symbol.getBinding());
    }

    @Test
    void roundTripsTheExistingActivityFixtureIncludingReferenceDefaultsAndArgumentDecalibration() throws Exception {
        ProcessingDefinition original = fixture("processing_definitions_activities.xml");
        ProcessingDefinition loaded = roundTrip(original);

        assertEquals(original.getActivityDefinitions().size(), loaded.getActivityDefinitions().size());
        ActivityProcessingDefinition activity = activity(loaded, 1001);
        PlainArgumentDefinition argument = assertInstanceOf(PlainArgumentDefinition.class, activity.getArgumentByName("ARG4"));
        ReferenceDefaultValue reference = assertInstanceOf(ReferenceDefaultValue.class, argument.getDefaultValue());
        assertSame(parameter(loaded, 108), reference.getParameter());
        assertEquals(DefaultValueType.ENGINEERING, reference.getType());
        assertEquals(DefaultValueType.RAW, reference.getTargetValueType());
        InvertedEnumCalibration decalibration = assertInstanceOf(InvertedEnumCalibration.class, argument.getDecalibration());
        assertEquals(11L, decalibration.calibrate("ON", null, ValueTypeEnum.ENUMERATED));
        assertTrue(argument.getChecks().get(0).isRawValueChecked());
        PlainArgumentDefinition fixedArgument = assertInstanceOf(PlainArgumentDefinition.class, activity.getArgumentByName("ARG3"));
        assertTrue(fixedArgument.isFixed());
        assertEquals("12.4", assertInstanceOf(FixedDefaultValue.class, fixedArgument.getDefaultValue()).getValue());
        assertEquals("spacecraft-id", activity.getProperties().get(0).getKey());
        assertEquals("101", activity.getProperties().get(0).getValue());
        assertEquals(5, activity(loaded, 1002).getVerificationTimeout());
        assertEquals("PARAM3.value() == self.argument(\"ARG1\")", activity(loaded, 1002).getVerification().getExpression());
        assertEquals(103, activity(loaded, 1002).getVerification().getSymbols().get(0).getReference());
        assertEquals(2, activity(loaded, 1003).getTransmissionTimeout());
        assertEquals(3, activity(loaded, 1003).getExecutionTimeout());
    }

    private static void assertUsefulDefaults(ProcessingDefinition definition) {
        assertTrue(definition.isSyntheticParameterProcessingEnabled());
        assertFalse(definition.isMirrored());
        assertNull(definition.getPathPrefix());
        ParameterProcessingDefinition parameter = parameter(definition, 1);
        assertEquals("", parameter.getDescription());
        assertEquals("", parameter.getUnit());
        assertFalse(parameter.isUserParameter());
        assertFalse(parameter.isWeakConsistency());
        assertTrue(parameter.getCalibrations().isEmpty());
        assertTrue(parameter.getChecks().isEmpty());
        EventProcessingDefinition event = event(definition, 2);
        assertEquals(Severity.INFO, event.getSeverity());
        assertTrue(event.isLogEnabled());
        assertEquals(0, event.getInhibitionPeriod());
        ActivityProcessingDefinition activity = activity(definition, 3);
        assertEquals(1000, activity.getExpectedDuration());
        assertEquals(0, activity.getTransmissionTimeout());
        assertEquals(0, activity.getExecutionTimeout());
        assertEquals(0, activity.getVerificationTimeout());
        assertFalse(assertInstanceOf(PlainArgumentDefinition.class, activity.getArgumentByName("VALUE")).isFixed());
        assertEquals(ExpressionDialect.GROOVY, activity.getVerification().getDialect());
        assertEquals(PropertyBinding.OBJECT, activity.getVerification().getSymbols().get(0).getBinding());
    }

    private static ProcessingDefinition referencedDefinition() {
        ParameterProcessingDefinition parameter = new ParameterProcessingDefinition();
        parameter.setId(Integer.MAX_VALUE);
        parameter.setLocation("ROOT.PARAM");
        parameter.setDescription(UNICODE_TEXT);
        parameter.setRawType(ValueTypeEnum.SIGNED_INTEGER);
        parameter.setEngineeringType(ValueTypeEnum.REAL);
        parameter.setUnit("µA & <nominal>");
        parameter.setDefaultValue(new FixedDefaultValue(DefaultValueType.RAW, "42"));
        parameter.setCalibrations(List.of(new ExpressionCalibration(new ExpressionDefinition("input * 2 + 0.5", List.of()))));
        parameter.setChecks(List.of(new ExpressionCheck("negative", CheckSeverity.WARNING, 2,
                new ExpressionDefinition("input < 0", List.of()))));

        ParameterProcessingDefinition derived = new ParameterProcessingDefinition();
        derived.setId(42);
        derived.setLocation("ROOT.DERIVED");
        derived.setRawType(ValueTypeEnum.CHARACTER_STRING);
        derived.setEngineeringType(ValueTypeEnum.CHARACTER_STRING);
        derived.setExpression(new ExpressionDefinition(SYNTHETIC_EXPRESSION,
                List.of(new SymbolDefinition("SOURCE", parameter.getId(), PropertyBinding.OBJECT))));

        EventProcessingDefinition event = new EventProcessingDefinition();
        event.setId(200);
        event.setLocation("ROOT.EVENT");
        event.setSeverity(Severity.WARN);
        event.setType("GROUND");
        parameter.setTriggers(List.of(new ParameterTriggerDefinition(event, TriggerCondition.ON_ALARM_RAISED)));

        ActivityProcessingDefinition activity = new ActivityProcessingDefinition();
        activity.setId(300);
        activity.setLocation("ROOT.SET");
        activity.setType("TC");
        activity.setArguments(List.of(
                new PlainArgumentDefinition("TARGET", ValueTypeEnum.SIGNED_INTEGER, ValueTypeEnum.REAL, "µA", false,
                        new ReferenceDefaultValue(DefaultValueType.ENGINEERING, parameter, DefaultValueType.ENGINEERING), null, List.of()),
                new PlainArgumentDefinition("LABEL", ValueTypeEnum.CHARACTER_STRING, ValueTypeEnum.CHARACTER_STRING, "", false,
                        new FixedDefaultValue(DefaultValueType.ENGINEERING, UNICODE_TEXT), null, List.of())));
        activity.setProperties(List.of(new KeyValue("display", UNICODE_TEXT)));
        activity.setVerification(new ExpressionDefinition("VALUE > 0 && VALUE < 100",
                List.of(new SymbolDefinition("VALUE", parameter.getId(), PropertyBinding.ENG_VALUE))));
        ParameterSetterDefinition setter = new ParameterSetterDefinition();
        setter.setActivity(activity);
        setter.setSetArgument("TARGET");
        parameter.setSetter(setter);

        ProcessingDefinition definition = new ProcessingDefinition(List.of(parameter, derived), List.of(event), List.of(activity));
        definition.setSyntheticParameterProcessingEnabled(false);
        return definition;
    }

    private static byte[] save(ProcessingDefinition definition) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ProcessingDefinition.save(definition, output);
        return output.toByteArray();
    }

    private static ProcessingDefinition roundTrip(ProcessingDefinition definition) throws Exception {
        return ProcessingDefinition.load(new ByteArrayInputStream(save(definition)));
    }

    private static ProcessingDefinition fixture(String resource) throws Exception {
        try (InputStream input = ProcessingDefinitionTest.class.getResourceAsStream("/" + resource)) {
            assertNotNull(input, "Missing processing definition fixture: " + resource);
            return ProcessingDefinition.load(input);
        }
    }

    private static ParameterProcessingDefinition parameter(ProcessingDefinition definition, int id) {
        return definition.getParameterDefinitions().stream().filter(parameter -> parameter.getId() == id).findFirst().orElseThrow();
    }

    private static EventProcessingDefinition event(ProcessingDefinition definition, int id) {
        return definition.getEventDefinitions().stream().filter(event -> event.getId() == id).findFirst().orElseThrow();
    }

    private static ActivityProcessingDefinition activity(ProcessingDefinition definition, int id) {
        return definition.getActivityDefinitions().stream().filter(activity -> activity.getId() == id).findFirst().orElseThrow();
    }

    private static Element element(Document document, String name) {
        return (Element) document.getElementsByTagName(name).item(0);
    }
}
