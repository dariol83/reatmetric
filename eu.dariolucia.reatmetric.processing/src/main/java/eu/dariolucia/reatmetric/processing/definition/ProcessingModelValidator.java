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

import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import groovy.lang.GroovyShell;

import java.util.*;

/**
 * Validator for {@link ProcessingDefinition} instances.
 */
public final class ProcessingModelValidator {

    private ProcessingModelValidator() {
        // Utility class
    }

    /**
     * Validate the provided processing definition and return all detected issues.
     *
     * @param definition the processing definition to validate
     * @return the list of detected validation issues, empty if the definition is valid
     */
    public static List<ProcessingModelValidationIssue> validate(ProcessingDefinition definition) {
        List<ProcessingModelValidationIssue> issues = new LinkedList<>();
        if(definition == null) {
            issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.MISSING_REFERENCED_ENTITY, "processing", "Processing definition is null"));
            return issues;
        }

        Map<Integer, AbstractProcessingDefinition> entitiesById = new HashMap<>();
        Set<Integer> parameterIds = new HashSet<>();
        Set<Integer> eventIds = new HashSet<>();
        Set<Integer> activityIds = new HashSet<>();

        validateDuplicateEntities(definition, entitiesById, parameterIds, eventIds, activityIds, issues);
        validateParameters(definition, entitiesById, parameterIds, eventIds, activityIds, issues);
        validateEvents(definition, entitiesById, issues);
        validateActivities(definition, entitiesById, parameterIds, issues);

        return issues;
    }

    private static void validateDuplicateEntities(ProcessingDefinition definition, Map<Integer, AbstractProcessingDefinition> entitiesById,
                                                  Set<Integer> parameterIds, Set<Integer> eventIds, Set<Integer> activityIds,
                                                  List<ProcessingModelValidationIssue> issues) {
        Map<String, AbstractProcessingDefinition> entitiesByPath = new HashMap<>();
        for(ParameterProcessingDefinition parameter : safeList(definition.getParameterDefinitions())) {
            registerEntity(parameter, "parameter", entitiesById, entitiesByPath, parameterIds, issues);
        }
        for(EventProcessingDefinition event : safeList(definition.getEventDefinitions())) {
            registerEntity(event, "event", entitiesById, entitiesByPath, eventIds, issues);
        }
        for(ActivityProcessingDefinition activity : safeList(definition.getActivityDefinitions())) {
            registerEntity(activity, "activity", entitiesById, entitiesByPath, activityIds, issues);
        }
    }

    private static void registerEntity(AbstractProcessingDefinition definition, String type, Map<Integer, AbstractProcessingDefinition> entitiesById,
                                       Map<String, AbstractProcessingDefinition> entitiesByPath, Set<Integer> typedIds,
                                       List<ProcessingModelValidationIssue> issues) {
        if(definition == null) {
            return;
        }
        Integer id = idOf(definition);
        String location = definitionLocation(type, definition);
        if(id != null) {
            AbstractProcessingDefinition previous = entitiesById.putIfAbsent(id, definition);
            if(previous != null) {
                issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.DUPLICATE_ENTITY_ID, location,
                        "Entity ID " + id + " is already used by " + definitionLocation("entity", previous)));
            }
            typedIds.add(id);
        }
        String path = definition.getLocation();
        if(path != null) {
            AbstractProcessingDefinition previous = entitiesByPath.putIfAbsent(path, definition);
            if(previous != null) {
                issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.DUPLICATE_ENTITY_PATH, location,
                        "Entity path " + path + " is already used by " + definitionLocation("entity", previous)));
            }
        }
    }

    private static void validateParameters(ProcessingDefinition definition, Map<Integer, AbstractProcessingDefinition> entitiesById,
                                           Set<Integer> parameterIds, Set<Integer> eventIds, Set<Integer> activityIds,
                                           List<ProcessingModelValidationIssue> issues) {
        for(ParameterProcessingDefinition parameter : safeList(definition.getParameterDefinitions())) {
            if(parameter == null) {
                continue;
            }
            String parameterLocation = definitionLocation("parameter", parameter);
            validateFixedDefaultValue(parameter.getDefaultValue(), parameter.getRawType(), parameter.getEngineeringType(), parameterLocation + "/default_value", issues);
            validateExpression(parameter.getExpression(), parameterLocation + "/synthetic", entitiesById, issues);
            validateValidityCondition(parameter.getValidity(), parameterLocation + "/validity", entitiesById, parameterIds, issues);
            for(int i = 0; i < safeList(parameter.getCalibrations()).size(); ++i) {
                validateCalibration(safeList(parameter.getCalibrations()).get(i), parameterLocation + "/calibrations[" + i + "]", entitiesById, parameterIds, issues);
            }
            for(int i = 0; i < safeList(parameter.getChecks()).size(); ++i) {
                validateCheck(safeList(parameter.getChecks()).get(i), parameterLocation + "/checks[" + i + "]", entitiesById, parameterIds, issues);
            }
            for(int i = 0; i < safeList(parameter.getTriggers()).size(); ++i) {
                ParameterTriggerDefinition trigger = safeList(parameter.getTriggers()).get(i);
                String triggerLocation = parameterLocation + "/triggers[" + i + "]";
                if(trigger != null) {
                    validateRequiredReferencedDefinition(trigger.getEvent(), eventIds, ProcessingModelValidationIssueType.MISSING_REFERENCED_ENTITY,
                            triggerLocation, "Referenced trigger event is missing from the processing definition", issues);
                }
            }
            validateSetter(parameter.getSetter(), parameterLocation + "/setter", entitiesById, parameterIds, activityIds, issues);
        }
    }

    private static void validateEvents(ProcessingDefinition definition, Map<Integer, AbstractProcessingDefinition> entitiesById,
                                       List<ProcessingModelValidationIssue> issues) {
        for(EventProcessingDefinition event : safeList(definition.getEventDefinitions())) {
            if(event != null) {
                validateExpression(event.getCondition(), definitionLocation("event", event) + "/condition", entitiesById, issues);
            }
        }
    }

    private static void validateActivities(ProcessingDefinition definition, Map<Integer, AbstractProcessingDefinition> entitiesById,
                                           Set<Integer> parameterIds, List<ProcessingModelValidationIssue> issues) {
        for(ActivityProcessingDefinition activity : safeList(definition.getActivityDefinitions())) {
            if(activity == null) {
                continue;
            }
            String activityLocation = definitionLocation("activity", activity);
            validateArgumentList(activity.getArguments(), activityLocation + "/arguments", entitiesById, parameterIds, issues);
            validateExpression(activity.getVerification(), activityLocation + "/verification", entitiesById, issues);
        }
    }

    private static void validateSetter(ParameterSetterDefinition setter, String location, Map<Integer, AbstractProcessingDefinition> entitiesById,
                                       Set<Integer> parameterIds, Set<Integer> activityIds, List<ProcessingModelValidationIssue> issues) {
        if(setter == null) {
            return;
        }
        validateRequiredReferencedDefinition(setter.getActivity(), activityIds, ProcessingModelValidationIssueType.MISSING_REFERENCED_ENTITY,
                location, "Referenced setter activity is missing from the processing definition", issues);
        validateCalibration(setter.getDecalibration(), location + "/decalibration", entitiesById, parameterIds, issues);
    }

    private static void validateArgumentList(List<AbstractArgumentDefinition> arguments, String location, Map<Integer, AbstractProcessingDefinition> entitiesById,
                                             Set<Integer> parameterIds, List<ProcessingModelValidationIssue> issues) {
        Map<String, AbstractArgumentDefinition> argumentsByName = new HashMap<>();
        for(int i = 0; i < safeList(arguments).size(); ++i) {
            AbstractArgumentDefinition argument = safeList(arguments).get(i);
            String argumentLocation = location + "[" + i + "]";
            if(argument == null) {
                continue;
            }
            String name = argument.getName();
            if(name != null) {
                AbstractArgumentDefinition previous = argumentsByName.putIfAbsent(name, argument);
                if(previous != null) {
                    issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.DUPLICATE_ACTIVITY_ARGUMENT_NAME,
                            argumentLocation, "Argument name " + name + " is already defined in the same activity argument scope"));
                }
            }
            if(argument instanceof PlainArgumentDefinition) {
                PlainArgumentDefinition plain = (PlainArgumentDefinition) argument;
                validateDefaultValue(plain.getDefaultValue(), plain.getRawType(), plain.getEngineeringType(), argumentLocation + "/" + name + "/default", parameterIds, issues);
                validateCalibration(plain.getDecalibration(), argumentLocation + "/" + name + "/decalibration", entitiesById, parameterIds, issues);
                for(int j = 0; j < safeList(plain.getChecks()).size(); ++j) {
                    validateCheck(safeList(plain.getChecks()).get(j), argumentLocation + "/" + name + "/checks[" + j + "]", entitiesById, parameterIds, issues);
                }
            } else if(argument instanceof ArrayArgumentDefinition) {
                validateArgumentList(((ArrayArgumentDefinition) argument).getElements(), argumentLocation + "/" + name + "/elements", entitiesById, parameterIds, issues);
            }
        }
    }

    private static void validateDefaultValue(AbstractDefaultValue defaultValue, ValueTypeEnum rawType, ValueTypeEnum engineeringType, String location,
                                             Set<Integer> parameterIds, List<ProcessingModelValidationIssue> issues) {
        if(defaultValue == null) {
            return;
        }
        if(defaultValue instanceof FixedDefaultValue) {
            validateFixedDefaultValue((FixedDefaultValue) defaultValue, rawType, engineeringType, location, issues);
        } else if(defaultValue instanceof ReferenceDefaultValue) {
            ReferenceDefaultValue reference = (ReferenceDefaultValue) defaultValue;
            if(reference.getType() == null || reference.getTargetValueType() == null) {
                issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.NULL_DEFAULT_VALUE, location,
                        "Reference default value type and target value type must be defined"));
            }
            validateRequiredReferencedDefinition(reference.getParameter(), parameterIds, ProcessingModelValidationIssueType.MISSING_REFERENCED_ENTITY,
                    location, "Referenced default value parameter is missing from the processing definition", issues);
        } else {
            issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.UNSUPPORTED_DEFAULT_VALUE, location,
                    "Unsupported default value type " + defaultValue.getClass().getName()));
        }
    }

    private static void validateFixedDefaultValue(FixedDefaultValue defaultValue, ValueTypeEnum rawType, ValueTypeEnum engineeringType, String location,
                                                  List<ProcessingModelValidationIssue> issues) {
        if(defaultValue == null) {
            return;
        }
        if(defaultValue.getType() == null || defaultValue.getValue() == null) {
            issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.NULL_DEFAULT_VALUE, location,
                    "Fixed default value type and value must be defined"));
            return;
        }
        ValueTypeEnum typeToParse = defaultValue.getType() == DefaultValueType.RAW ? rawType : engineeringType;
        if(typeToParse == null || typeToParse == ValueTypeEnum.EXTENSION || typeToParse == ValueTypeEnum.DERIVED) {
            issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.UNSUPPORTED_DEFAULT_VALUE, location,
                    "Default value cannot be parsed using value type " + typeToParse));
            return;
        }
        try {
            ValueUtil.parse(typeToParse, defaultValue.getValue());
        } catch (RuntimeException e) {
            issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.UNPARSEABLE_DEFAULT_VALUE, location,
                    "Default value '" + defaultValue.getValue() + "' cannot be parsed as " + typeToParse + ": " + e.getMessage()));
        }
    }

    private static void validateCalibration(CalibrationDefinition calibration, String location, Map<Integer, AbstractProcessingDefinition> entitiesById,
                                            Set<Integer> parameterIds, List<ProcessingModelValidationIssue> issues) {
        if(calibration == null) {
            return;
        }
        validateValidityCondition(calibration.getApplicability(), location + "/applicability", entitiesById, parameterIds, issues);
        if(calibration instanceof XYCalibration) {
            validateXyCalibration((XYCalibration) calibration, location, issues);
        } else if(calibration instanceof ExpressionCalibration) {
            validateExpression(((ExpressionCalibration) calibration).getDefinition(), location + "/definition", entitiesById, issues);
        }
    }

    private static void validateCheck(CheckDefinition check, String location, Map<Integer, AbstractProcessingDefinition> entitiesById,
                                      Set<Integer> parameterIds, List<ProcessingModelValidationIssue> issues) {
        if(check == null) {
            return;
        }
        validateValidityCondition(check.getApplicability(), location + "/applicability", entitiesById, parameterIds, issues);
        if(check instanceof ExpressionCheck) {
            validateExpression(((ExpressionCheck) check).getDefinition(), location + "/definition", entitiesById, issues);
        }
    }

    private static void validateXyCalibration(XYCalibration calibration, String location, List<ProcessingModelValidationIssue> issues) {
        List<XYCalibrationPoint> points = calibration.getPoints();
        if(points == null || points.size() < 2) {
            issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.INVALID_XY_CALIBRATION_POINTS, location,
                    "XY calibration requires at least two points"));
            return;
        }
        Set<Double> xValues = new HashSet<>();
        for(int i = 0; i < points.size(); ++i) {
            XYCalibrationPoint point = points.get(i);
            if(point == null || !Double.isFinite(point.getX()) || !Double.isFinite(point.getY())) {
                issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.INVALID_XY_CALIBRATION_POINTS, location + "/point[" + i + "]",
                        "XY calibration point must be non-null and contain finite X and Y values"));
            } else if(!xValues.add(point.getX())) {
                issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.INVALID_XY_CALIBRATION_POINTS, location + "/point[" + i + "]",
                        "XY calibration contains duplicate X value " + point.getX()));
            }
        }
    }

    private static void validateValidityCondition(ValidityCondition condition, String location, Map<Integer, AbstractProcessingDefinition> entitiesById,
                                                  Set<Integer> parameterIds, List<ProcessingModelValidationIssue> issues) {
        if(condition == null) {
            return;
        }
        validateExpression(condition.getCondition(), location + "/condition", entitiesById, issues);
        validateMatcher(condition.getMatch(), location + "/match", parameterIds, issues);
    }

    private static void validateMatcher(MatcherDefinition matcher, String location, Set<Integer> parameterIds, List<ProcessingModelValidationIssue> issues) {
        if(matcher == null) {
            return;
        }
        validateRequiredReferencedDefinition(matcher.getParameter(), parameterIds, ProcessingModelValidationIssueType.MISSING_REFERENCED_ENTITY,
                location, "Referenced matcher parameter is missing from the processing definition", issues);
        validateReferencedDefinition(matcher.getReference(), parameterIds, ProcessingModelValidationIssueType.MISSING_REFERENCED_ENTITY,
                location, "Referenced matcher comparison parameter is missing from the processing definition", issues);
    }

    private static void validateExpression(ExpressionDefinition expression, String location, Map<Integer, AbstractProcessingDefinition> entitiesById,
                                           List<ProcessingModelValidationIssue> issues) {
        if(expression == null) {
            return;
        }
        for(SymbolDefinition symbol : safeList(expression.getSymbols())) {
            if(symbol == null) {
                continue;
            }
            Integer reference = referenceOf(symbol);
            if(reference == null || !entitiesById.containsKey(reference)) {
                issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.MISSING_REFERENCED_ENTITY, location + "/symbol/" + symbol.getName(),
                        "Expression symbol references missing entity ID " + reference));
            }
        }
        if(expression.getDialect() == null || expression.getDialect() != ExpressionDialect.GROOVY) {
            issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.UNSUPPORTED_EXPRESSION_DIALECT, location,
                    "Expression dialect is not supported: " + expression.getDialect()));
            return;
        }
        if(expression.getExpression() == null) {
            issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.GROOVY_EXPRESSION_SYNTAX_ERROR, location,
                    "Groovy expression text is null"));
            return;
        }
        try {
            new GroovyShell().parse(expression.getExpression());
        } catch (RuntimeException e) {
            issues.add(new ProcessingModelValidationIssue(ProcessingModelValidationIssueType.GROOVY_EXPRESSION_SYNTAX_ERROR, location,
                    "Groovy expression cannot be parsed: " + e.getMessage()));
        }
    }

    private static void validateReferencedDefinition(AbstractProcessingDefinition referencedDefinition, Set<Integer> allowedIds,
                                                     ProcessingModelValidationIssueType issueType, String location, String message,
                                                     List<ProcessingModelValidationIssue> issues) {
        if(referencedDefinition == null) {
            return;
        }
        Integer id = idOf(referencedDefinition);
        if(id == null || !allowedIds.contains(id)) {
            issues.add(new ProcessingModelValidationIssue(issueType, location, message + ": " + definitionLocation("entity", referencedDefinition)));
        }
    }

    private static void validateRequiredReferencedDefinition(AbstractProcessingDefinition referencedDefinition, Set<Integer> allowedIds,
                                                            ProcessingModelValidationIssueType issueType, String location, String message,
                                                            List<ProcessingModelValidationIssue> issues) {
        if(referencedDefinition == null) {
            issues.add(new ProcessingModelValidationIssue(issueType, location, message + ": null"));
            return;
        }
        validateReferencedDefinition(referencedDefinition, allowedIds, issueType, location, message, issues);
    }

    private static Integer idOf(AbstractProcessingDefinition definition) {
        try {
            return definition.getId();
        } catch (NullPointerException e) {
            return null;
        }
    }

    private static Integer referenceOf(SymbolDefinition symbol) {
        try {
            return symbol.getReference();
        } catch (NullPointerException e) {
            return null;
        }
    }

    private static String definitionLocation(String type, AbstractProcessingDefinition definition) {
        if(definition == null) {
            return type + "[null]";
        }
        return type + "[id=" + idOf(definition) + ", location=" + definition.getLocation() + "]";
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
