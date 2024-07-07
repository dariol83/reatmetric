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

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.processing.input.SetParameterRequest;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import jakarta.xml.bind.annotation.*;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * This class defines the main characteristics (raw value type, engineering value type) and the processing to be applied
 * to a specific parameter:
 * <ol>
 *     <li>Validity computation, in case a {@link ValidityCondition} is defined</li>
 *     <li>Value computation, according to a specified {@link ExpressionDefinition} (if present)</li>
 *     <li>Calibration computation, in case a {@link CalibrationDefinition} is defined </li>
 *     <li>Check computation, in case one or more {@link CheckDefinition} are defined</li>
 * </ol>
 *
 * This class also contains information about:
 * <ul>
 *     <li>The way to set the value of the parameter using a specified activity via a {@link ParameterSetterDefinition}</li>
 *     <li>The list of events to be triggered based on parameter specific transitions via {@link ParameterTriggerDefinition} </li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ParameterProcessingDefinition extends AbstractProcessingDefinition implements Serializable {

    @XmlAttribute(name = "raw_type",required = true)
    private ValueTypeEnum rawType;

    @XmlAttribute(name = "eng_type", required = true)
    private ValueTypeEnum engineeringType;

    @XmlAttribute(name = "eng_unit")
    private String unit = "";

    @XmlAttribute(name = "log_repetition_period")
    private int logRepetitionPeriod = 0;

    @XmlAttribute(name = "user_parameter")
    private boolean userParameter = false;

    @XmlAttribute(name = "weak_consistency")
    private boolean weakConsistency = false;

    @XmlElement(name = "validity")
    private ValidityCondition validity;

    @XmlElement(name = "synthetic")
    private ExpressionDefinition expression;

    @XmlElement(name = "default_value")
    private FixedDefaultValue defaultValue = null;

    @XmlElements({
            @XmlElement(name="calib_xy",type=XYCalibration.class),
            @XmlElement(name="calib_poly",type=PolyCalibration.class),
            @XmlElement(name="calib_log",type=LogCalibration.class),
            @XmlElement(name="calib_enum",type=EnumCalibration.class),
            @XmlElement(name="calib_range_enum",type=RangeEnumCalibration.class),
            @XmlElement(name="calib_expression",type=ExpressionCalibration.class),
            @XmlElement(name="calib_external",type=ExternalCalibration.class),
    })
    private List<CalibrationDefinition> calibrations = new LinkedList<>();

    @XmlElementWrapper(name = "checks")
    @XmlElements({
            @XmlElement(name="limit",type=LimitCheck.class),
            @XmlElement(name="expected",type=ExpectedCheck.class),
            @XmlElement(name="delta",type=DeltaCheck.class),
            @XmlElement(name="expression",type=ExpressionCheck.class),
            @XmlElement(name="external",type=ExternalCheck.class),
    })
    private List<CheckDefinition> checks = new LinkedList<>();

    @XmlElement(name="setter")
    private ParameterSetterDefinition setter;

    @XmlElementWrapper(name = "triggers")
    @XmlElement(name="trigger")
    private List<ParameterTriggerDefinition> triggers = new LinkedList<>();

    /**
     * The raw value type of the parameter. This information is used to check the input as provided when injecting
     * {@link eu.dariolucia.reatmetric.api.processing.input.ParameterSample} objects into the Processing model.
     * <p></p>
     * Attribute: raw_type (mandatory)
     *
     * @return the raw value type of the parameter
     */
    public ValueTypeEnum getRawType() {
        return rawType;
    }

    public void setRawType(ValueTypeEnum rawType) {
        this.rawType = rawType;
    }

    /**
     * The engineering value type of the parameter. It must match the output of the assigned calibration, if provided.
     * <p></p>
     * Attribute: eng_type (mandatory)
     *
     * @return the engineering value type of the parameter
     */
    public ValueTypeEnum getEngineeringType() {
        return engineeringType;
    }

    public void setEngineeringType(ValueTypeEnum engineeringType) {
        this.engineeringType = engineeringType;
    }

    /**
     * The unit of the engineering value.
     * <p></p>
     * Attribute: unit
     *
     * @return the unit of the engineering value, or null if no unit can be specified
     */
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * The {@link ValidityCondition} that is used to derive the {@link eu.dariolucia.reatmetric.api.parameters.Validity} of a
     * parameter. It can be null.
     * <p></p>
     * Element: validity
     *
     * @return the defined {@link ValidityCondition} or null
     */
    public ValidityCondition getValidity() {
        return validity;
    }

    public void setValidity(ValidityCondition validity) {
        this.validity = validity;
    }

    /**
     * The ordered list of {@link CalibrationDefinition} to be applied to the injected raw value, in order to compute
     * the engineering value. Only one calibration is applied, i.e. the first one that results as applicable. If no
     * calibration is defined, then the raw value is directly translated into the engineering value.
     * <p></p>
     * Elements:
     * <ul>
     *     <li>calib_xy for {@link XYCalibration}</li>
     *     <li>calib_poly for {@link PolyCalibration}</li>
     *     <li>calib_log for {@link LogCalibration}</li>
     *     <li>calib_enum for {@link EnumCalibration}</li>
     *     <li>calib_range_enum for {@link RangeEnumCalibration}</li>
     *     <li>calib_expression for {@link ExpressionCalibration}</li>
     *     <li>calib_external for {@link ExternalCalibration}</li>
     * </ul>
     *
     * @return the list of calibrations (list can be empty)
     */
    public List<CalibrationDefinition> getCalibrations() {
        return calibrations;
    }

    public void setCalibrations(List<CalibrationDefinition> calibrations) {
        this.calibrations = calibrations;
    }

    /**
     * The ordered list of {@link CheckDefinition} to be verified against the engineering value (computed after calibration).
     * <p></p>
     * Elements: checks/
     * <ul>
     *     <li>limit for {@link LimitCheck}</li>
     *     <li>delta for {@link DeltaCheck}</li>
     *     <li>expected for {@link ExpectedCheck}</li>
     *     <li>expression for {@link ExpressionCheck}</li>
     *     <li>external for {@link ExternalCheck}</li>
     * </ul>
     *
     * @return the list of checks (list can be empty)
     */
    public List<CheckDefinition> getChecks() {
        return checks;
    }

    public void setChecks(List<CheckDefinition> checks) {
        this.checks = checks;
    }

    /**
     * The expression ({@link ExpressionDefinition} defining how the raw value shall be computed. The presence of such
     * expression automatically defines this parameter as 'synthetic' and no externally injected sample will be processed.
     * <p></p>
     * Element: expression
     *
     * @return the expression definition, or null
     */
    public ExpressionDefinition getExpression() {
        return expression;
    }

    public void setExpression(ExpressionDefinition expression) {
        this.expression = expression;
    }

    /**
     * The ordered list of {@link ParameterTriggerDefinition} that drives the reporting of events in the processing model,
     * depending on the outcome of the processing of the parameter.
     * <p></p>
     * Elements: triggers/trigger
     *
     * @return the list of triggers (list can be empty)
     */
    public List<ParameterTriggerDefinition> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<ParameterTriggerDefinition> triggers) {
        this.triggers = triggers;
    }

    /**
     * The value to be used, as initial, default value, for the parameter state. It can be null.
     * <p></p>
     * Element: default_value
     *
     * @return the default value, or null
     */
    public FixedDefaultValue getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(FixedDefaultValue defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * The reference to the activity that will be used by the processing model, when requesting to set the parameter
     * value via the {@link eu.dariolucia.reatmetric.api.processing.IProcessingModel#setParameterValue(SetParameterRequest)}
     * method. It can be null.
     * <p></p>
     * Element: setter
     *
     * @return the {@link ParameterTriggerDefinition} object, or null.
     */
    public ParameterSetterDefinition getSetter() {
        return setter;
    }

    public void setSetter(ParameterSetterDefinition setter) {
        this.setter = setter;
    }

    /**
     * The minimum log generation period in milliseconds. If an alarm generates a log within the minimum repetition period window,
     * the log message is skipped and a counter increased.
     * <p></p>
     * Attribute: log_repetition_period
     *
     * @return the minimum log repetition period in milliseconds
     */
    public int getLogRepetitionPeriod() {
        return logRepetitionPeriod;
    }

    public void setLogRepetitionPeriod(int logRepetitionPeriod) {
        this.logRepetitionPeriod = logRepetitionPeriod;
    }

    /**
     * Whether this parameter is a user-settable parameter or not. User-settable parameters are set as usual by the
     * setParameterValue() method of the {@link eu.dariolucia.reatmetric.api.processing.IProcessingModel}. This attribute
     * is used to mark a parameter as directly settable, without having a setter activity defined.
     *
     * @return true if the parameter is a user-settable parameter, otherwise false
     */
    public boolean isUserParameter() {
        return userParameter;
    }

    public void setUserParameter(boolean userParameter) {
        this.userParameter = userParameter;
    }

    /**
     * A weakly consistent parameter (applicable only for synthetic parameters) is a parameter that:
     * <ul>
     *     <li>will not be considered to compute overlaps in the working set processing of the processing model</li>
     *     <li>will not add itself and its dependant objects to the working set during normal (input-based) processing</li>
     * </ul>
     *
     * Weakly consistent parameters are computed at most according to the timeout expressed in the related attribute, in an
     * "inconsistent" way, i.e. access the status of the objects they depend on, without blocking the normal processing
     * of the working set. Their processing is organised independently of the processing of the inputs to the processing
     * model: when an inconsistent object is processed, all its dependant objects are also processed and this is done via
     * the standard update process of the processing models.
     * <br />
     * Only synthetic parameters can have this flag set to true. If this is set on other types of parameters, the flag is
     * simply ignored.
     *
     * @return true if the parameter is weakly consistent, otherwise false.
     */
    public boolean isWeakConsistency() {
        return weakConsistency;
    }

    public void setWeakConsistency(boolean weakConsistency) {
        this.weakConsistency = weakConsistency;
    }

    /**
     * Internally used by the processing model.
     *
     * @return the list of expected values (raw values)
     */
    public List<Object> buildExpectedValuesRaw() {
        if(getSetter() != null) {
            return buildExpectedValuesRawWithSetter();
        } else if(isUserParameter()) {
            return buildExpectedValuesRawWithUserParameter();
        } else {
            return null;
        }
    }

    private List<Object> buildExpectedValuesRawWithUserParameter() {
        // Only if there is a single calibration of type EnumCalibration: in theory we could support also
        // multiple calibrations (if all are of type EnumCalibration) but it is error-prone for the user
        if(getCalibrations().size() == 1 && getCalibrations().get(0) instanceof EnumCalibration) {
            EnumCalibration iec = (EnumCalibration) getCalibrations().get(0);
            List<Object> rawValues = new LinkedList<>();
            for(EnumCalibrationPoint p : iec.getPoints()) {
                if(getRawType() == ValueTypeEnum.ENUMERATED) {
                    rawValues.add(((Long) p.getInput()).intValue());
                } else {
                    rawValues.add(p.getInput());
                }
            }
            return rawValues;
        } else {
            return null;
        }
    }

    private List<Object> buildExpectedValuesRawWithSetter() {
        if(getSetter().getDecalibration() instanceof InvertedEnumCalibration) {
            InvertedEnumCalibration iec = (InvertedEnumCalibration) getSetter().getDecalibration();
            List<Object> rawValues = new LinkedList<>();
            for(InvertedEnumCalibrationPoint p : iec.getPoints()) {
                Object valueToAdd = p.getValue();
                if(getRawType() == ValueTypeEnum.ENUMERATED) {
                    valueToAdd = ((Long) valueToAdd).intValue();
                }
                rawValues.add(valueToAdd);
            }
            if(iec.getDefaultValue() != null) {
                Object valueToAdd = iec.getDefaultValue();
                if(getRawType() == ValueTypeEnum.ENUMERATED) {
                    valueToAdd = (iec.getDefaultValue()).intValue();
                }
                if(!rawValues.contains(valueToAdd)) {
                    rawValues.add(valueToAdd);
                }
            }
            return rawValues;
        }
        if(getSetter().getDecalibration() instanceof EnumCalibration) {
            EnumCalibration iec = (EnumCalibration) getSetter().getDecalibration();
            List<Object> rawValues = new LinkedList<>();
            for(EnumCalibrationPoint p : iec.getPoints()) {
                rawValues.add(p.getValue());
            }
            if(iec.getDefaultValue() != null && !rawValues.contains(iec.getDefaultValue())) {
                rawValues.add(iec.getDefaultValue());
            }
            return rawValues;
        }
        // Look for the activity definition
        PlainArgumentDefinition valueArgument = (PlainArgumentDefinition) getSetter().getActivity().getArgumentByName(getSetter().getSetArgument());
        if(valueArgument != null) {
            return valueArgument.buildExpectedValuesRaw();
        } else {
            return null;
        }
    }

    /**
     * Internally used by the processing model.
     *
     * @return the list of expected values (eng. values)
     */
    public List<Object> buildExpectedValuesEng() {
        if(getSetter() != null) {
            return buildExpectedValuesEngWithSetter();
        } else if(isUserParameter()) {
            return buildExpectedValuesEngWithUserParameter();
        } else {
            return null;
        }
    }

    private List<Object> buildExpectedValuesEngWithUserParameter() {
        // Only if there is a single calibration of type EnumCalibration: in theory we could support also
        // multiple calibrations (if all are of type EnumCalibration) but it is error-prone for the user
        if(getCalibrations().size() == 1 && getCalibrations().get(0) instanceof EnumCalibration) {
            EnumCalibration iec = (EnumCalibration) getCalibrations().get(0);
            List<Object> engValues = new LinkedList<>();
            for(EnumCalibrationPoint p : iec.getPoints()) {
                engValues.add(p.getValue());
            }
            return engValues;
        } else {
            return null;
        }
    }

    private List<Object> buildExpectedValuesEngWithSetter() {
        if(getSetter().getDecalibration() != null) {
            if(getSetter().getDecalibration() instanceof InvertedEnumCalibration) {
                InvertedEnumCalibration iec = (InvertedEnumCalibration) getSetter().getDecalibration();
                List<Object> engValues = new LinkedList<>();
                for(InvertedEnumCalibrationPoint p : iec.getPoints()) {
                    engValues.add(p.getInput());
                }
                return engValues;
            }
            if(getSetter().getDecalibration() instanceof EnumCalibration) {
                EnumCalibration iec = (EnumCalibration) getSetter().getDecalibration();
                List<Object> engValues = new LinkedList<>();
                for(EnumCalibrationPoint p : iec.getPoints()) {
                    Object valueToAdd =  p.getInput();
                    if(getEngineeringType() == ValueTypeEnum.ENUMERATED) {
                        valueToAdd = ((Long)  p.getInput()).intValue();
                    }
                    engValues.add(valueToAdd);
                }
                return engValues;
            }
        }
        // Look for the activity definition
        PlainArgumentDefinition valueArgument = (PlainArgumentDefinition) getSetter().getActivity().getArgumentByName(getSetter().getSetArgument());
        if(valueArgument != null) {
            return valueArgument.buildExpectedValuesEng();
        } else {
            return null;
        }
    }

    @Override
    public void preload() throws Exception {
        if(expression != null) {
            expression.preload();
        }
        for(CalibrationDefinition cd : calibrations) {
            cd.preload();
        }
        for(CheckDefinition cd : checks) {
            cd.preload();
        }
        if(validity != null) {
            validity.preload();
        }
    }
}
