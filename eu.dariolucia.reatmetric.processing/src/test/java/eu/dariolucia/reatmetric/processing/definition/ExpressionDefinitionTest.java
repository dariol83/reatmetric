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
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.processing.scripting.IEntityBinding;
import eu.dariolucia.reatmetric.api.processing.scripting.IEventBinding;
import eu.dariolucia.reatmetric.api.processing.scripting.IParameterBinding;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import org.junit.jupiter.api.Test;

import javax.script.ScriptException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionDefinitionTest {

    private static final Instant GENERATION_TIME = Instant.parse("2026-11-13T10:00:00Z");
    private static final Instant RECEPTION_TIME = GENERATION_TIME.plusSeconds(2);

    @Test
    void preservesLongArithmeticBeyondTheIntegerRange() throws ScriptException {
        ExpressionDefinition expression = expression("input * 3 + 1");

        assertEquals(27_000_000_001L,
                expression.execute(null, Map.of("input", 9_000_000_000L), ValueTypeEnum.SIGNED_INTEGER));
    }

    @Test
    void dividesIntegerInputsWithoutTruncatingTheFraction() throws ScriptException {
        ExpressionDefinition expression = expression("input / 4");

        assertEquals(new BigDecimal("1.25"),
                expression.execute(null, Map.of("input", 5L), ValueTypeEnum.REAL));
    }

    @Test
    void distinguishesParameterRawAndEngineeringValueBindings() throws ScriptException {
        ParameterBinding parameter = parameter(10L, 25.5);
        ExpressionDefinition expression = expression("engineering - raw",
                new SymbolDefinition("raw", 1, PropertyBinding.SOURCE_VALUE),
                new SymbolDefinition("engineering", 1, PropertyBinding.ENG_VALUE));

        assertEquals(15.5, ((Number) expression.execute(id -> parameter, null, ValueTypeEnum.REAL)).doubleValue());
    }

    @Test
    void callsBoundParameterMethodsAndUsesJavaTimeAndEnumProperties() throws ScriptException {
        ParameterBinding parameter = parameter(10L, 25.5);
        ExpressionDefinition expression = expression(
                "sensor.valid() && !sensor.inAlarm() && sensor.rawValue() == 10L && sensor.value() == 25.5 " +
                        "&& path == sensor.path() && path.endsWith('.P1') " +
                        "&& received.isAfter(generated) && state.name() == 'NOMINAL' && validity.name() == 'VALID'",
                new SymbolDefinition("sensor", 1, PropertyBinding.OBJECT),
                new SymbolDefinition("path", 1, PropertyBinding.PATH),
                new SymbolDefinition("generated", 1, PropertyBinding.GEN_TIME),
                new SymbolDefinition("received", 1, PropertyBinding.RCT_TIME),
                new SymbolDefinition("state", 1, PropertyBinding.ALARM_STATE),
                new SymbolDefinition("validity", 1, PropertyBinding.VALIDITY));

        assertEquals(Boolean.TRUE, expression.execute(id -> parameter, null, ValueTypeEnum.BOOLEAN));
    }

    @Test
    void exposesEventPropertiesAndAllowsJavaReportAccess() throws ScriptException {
        EventBinding event = new EventBinding(2, "DEVICE-A", "OVERTEMP", Map.of("temperature", 101L));
        ExpressionDefinition expression = expression(
                "source == 'DEVICE-A' && qualifier == 'OVERTEMP' && route == 'TM' " +
                        "&& event.severity().name() == 'WARN' && event.report().get('temperature') > 100",
                new SymbolDefinition("event", 2, PropertyBinding.OBJECT),
                new SymbolDefinition("source", 2, PropertyBinding.SOURCE),
                new SymbolDefinition("qualifier", 2, PropertyBinding.QUALIFIER),
                new SymbolDefinition("route", 2, PropertyBinding.ROUTE));

        assertEquals(Boolean.TRUE, expression.execute(id -> event, null, ValueTypeEnum.BOOLEAN));
    }

    @Test
    void preloadedExpressionUsesFreshEntityAndAdditionalBindingsOnEveryExecution() throws ScriptException {
        Map<Integer, IEntityBinding> entities = new HashMap<>();
        entities.put(1, parameter(3L, 30L));
        ExpressionDefinition expression = expression("raw * scale",
                new SymbolDefinition("raw", 1, PropertyBinding.SOURCE_VALUE));
        expression.preload();

        assertEquals(6L, expression.execute(entities::get, Map.of("scale", 2L), ValueTypeEnum.SIGNED_INTEGER));

        entities.put(1, parameter(5L, 50L));
        assertEquals(20L, expression.execute(entities::get, Map.of("scale", 4L), ValueTypeEnum.SIGNED_INTEGER));
    }

    @Test
    void calibrationCombinesItsCurrentInputWithAReferencedGain() throws Exception {
        ParameterBinding gain = parameter(2L, 1.5);
        ExpressionCalibration calibration = new ExpressionCalibration(expression("(input * gain + 0.25) as double",
                new SymbolDefinition("gain", 1, PropertyBinding.ENG_VALUE)));
        calibration.preload();

        assertEquals(6.25, ((Number) calibration.calibrate(4L, id -> gain, ValueTypeEnum.REAL)).doubleValue());
        assertEquals(12.25, ((Number) calibration.calibrate(8L, id -> gain, ValueTypeEnum.REAL)).doubleValue());
    }

    @Test
    void expressionCheckUsesItsInputAndTheConfiguredViolationThreshold() throws CheckException {
        ParameterBinding limit = parameter(10L, 10L);
        ExpressionCheck check = new ExpressionCheck("high", CheckSeverity.WARNING, 2,
                expression("input > limit", new SymbolDefinition("limit", 1, PropertyBinding.ENG_VALUE)));

        assertEquals(AlarmState.NOMINAL, check.check(9L, GENERATION_TIME, 0, id -> limit));
        assertEquals(AlarmState.VIOLATED, check.check(11L, GENERATION_TIME, 0, id -> limit));
        assertEquals(AlarmState.WARNING, check.check(11L, GENERATION_TIME, 1, id -> limit));
        assertEquals(AlarmState.NOMINAL, check.check(9L, GENERATION_TIME, 2, id -> limit));
    }

    @Test
    void validityConditionFollowsTheCurrentParameterStatus() throws ValidityException {
        ValidityCondition condition = new ValidityCondition(expression("sensor.valid() && sensor.value() > 0",
                new SymbolDefinition("sensor", 1, PropertyBinding.OBJECT)));

        assertTrue(condition.execute(id -> parameter(1L, 10L)));
        assertFalse(condition.execute(id -> new ParameterBinding(1, 1L, 10L, Validity.INVALID, AlarmState.NOMINAL)));
        assertFalse(condition.execute(id -> parameter(0L, 0L)));
    }

    @Test
    void reportsInvalidExpressionSyntaxAsAScriptException() {
        ExpressionDefinition expression = expression("if (");

        ScriptException exception = assertThrows(ScriptException.class,
                () -> expression.execute(null, null, ValueTypeEnum.BOOLEAN));

        assertNotNull(exception.getCause());
        assertFalse(exception.getMessage().isBlank());
    }

    @Test
    void preservesTheCauseOfAJavaMethodFailureAsAScriptException() {
        ExpressionDefinition expression = expression("Integer.parseInt(input)");

        ScriptException exception = assertThrows(ScriptException.class,
                () -> expression.execute(null, Map.of("input", "invalid"), ValueTypeEnum.SIGNED_INTEGER));

        assertInstanceOf(NumberFormatException.class, exception.getCause());
    }

    @Test
    void expressionCheckRejectsANonBooleanResultInsteadOfApplyingGroovyTruth() {
        ExpressionCheck check = new ExpressionCheck("invalid", CheckSeverity.ALARM, 1, expression("'nonempty'"));

        CheckException exception = assertThrows(CheckException.class,
                () -> check.check(1L, GENERATION_TIME, 0, null));

        assertInstanceOf(ClassCastException.class, exception.getCause());
    }

    private static ExpressionDefinition expression(String script, SymbolDefinition... symbols) {
        return new ExpressionDefinition(script, Arrays.asList(symbols));
    }

    private static ParameterBinding parameter(Object rawValue, Object value) {
        return new ParameterBinding(1, rawValue, value, Validity.VALID, AlarmState.NOMINAL);
    }

    public record ParameterBinding(long id, Object rawValue, Object value, Validity validity,
                                   AlarmState alarmState) implements IParameterBinding {

        @Override
        public String path() {
            return "ROOT.P" + id;
        }

        @Override
        public Instant generationTime() {
            return GENERATION_TIME;
        }

        @Override
        public Instant receptionTime() {
            return RECEPTION_TIME;
        }

        @Override
        public boolean inAlarm() {
            return alarmState.isAlarm();
        }

        @Override
        public boolean valid() {
            return validity == Validity.VALID;
        }

        @Override
        public Long containerId() {
            return 42L;
        }

        @Override
        public String route() {
            return "TM";
        }
    }

    public record EventBinding(long id, String source, String qualifier, Object report) implements IEventBinding {

        @Override
        public String path() {
            return "ROOT.E" + id;
        }

        @Override
        public Instant generationTime() {
            return GENERATION_TIME;
        }

        @Override
        public Instant receptionTime() {
            return RECEPTION_TIME;
        }

        @Override
        public Severity severity() {
            return Severity.WARN;
        }

        @Override
        public String route() {
            return "TM";
        }

        @Override
        public String type() {
            return "DEVICE";
        }

        @Override
        public Long containerId() {
            return 42L;
        }
    }
}
