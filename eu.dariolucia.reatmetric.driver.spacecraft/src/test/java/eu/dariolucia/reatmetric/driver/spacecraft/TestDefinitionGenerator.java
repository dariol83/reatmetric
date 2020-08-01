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

package eu.dariolucia.reatmetric.driver.spacecraft;

import eu.dariolucia.ccsds.encdec.definition.*;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.processing.definition.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestDefinitionGenerator {

    private static final char[] PREFIX_COUNTER = new char[] {'A', 'A', 'A'};
    private static int ID_COUNTER = 100000;

    public static void main(String[] args) throws IOException {
        if(args.length != 1) {
            System.err.println("Usage: TestDefinitionGenerator <path to folder>");
            System.exit(1);
        }

        // Start with the generation of the processing definitions
        ProcessingDefinition defs = generateProcessingDefinitions();
        storeDefinitions(defs, args[0]);

        // Generate the TM/TC packet definitions
        Definition packetDefs = generatePacketDefinitions(defs);
        storePacketDefinitions(packetDefs, args[0]);
    }

    private static Definition generatePacketDefinitions(ProcessingDefinition defs) {
        Definition d = new Definition();
        // Create the parameter definitions
        Map<ParameterProcessingDefinition, ParameterDefinition> proc2param = new HashMap<>();
        for(ParameterProcessingDefinition ppd : defs.getParameterDefinitions()) {
            if(ppd.getExpression() != null) {
                continue;
            }
            ParameterDefinition pd = new ParameterDefinition();
            pd.setDescription(ppd.getDescription());
            pd.setId(extractName(ppd.getLocation()));
            pd.setType(mapType(ppd.getRawType()));
            pd.setExternalId(ppd.getId() - 100000);
            d.getParameters().add(pd);
            proc2param.put(ppd, pd);
        }
        System.out.println("Mapped " + d.getParameters().size() + " onboard parameters");
        // Now create the identification fields
        IdentField ifApid = new IdentField("I-APID", 0, 16, 2047, 0, 0, 0);
        IdentField ifPType = new IdentField("I-PUS-TYPE", 7, 1, -1, 0, 0, 0);
        IdentField ifPSubtype = new IdentField("I-PUS-SUBTYPE", 8, 1, -1, 0, 0, 0);
        IdentField ifP1 = new IdentField("I-P1", 16, 1, -1, 0, 0, 0);

        d.getIdentificationFields().addAll(Arrays.asList(ifApid, ifPType, ifPSubtype, ifP1));

        Queue<ParameterProcessingDefinition> parametersToAdd = new LinkedList<>();
        System.out.println("Processing " + defs.getParameterDefinitions().size() + " parameters");
        for(ParameterProcessingDefinition ppd : defs.getParameterDefinitions()) {
            if(ppd.getExpression() == null) {
                parametersToAdd.add(ppd);
            }
        }
        System.out.println("Number of TM parameters to encode in 3,25 packets: " + parametersToAdd.size());
        int commonSelector = 0;
        List<ParameterProcessingDefinition> commonPool = new ArrayList<>();
        for(int i = 0; i < 100; ++i) {
            commonPool.add(defs.getParameterDefinitions().get(i * 7));
        }
        commonPool = commonPool.stream().filter(pd -> pd.getExpression() == null).collect(Collectors.toList());

        // Now the packets: create 1620 PUS 3,25 using 5 templates (324 cycles). SID is I-P1, location of parameters is absolute.
        // Number of parameters: 57, 42, 62, 47, 52 (260 parameters per cycle). Each APID covers 50 different packets with the SID.
        // Each packet contains 100 parameters from the pool of parameters, and 2 parameters from a common parameter pool of 100 parameters
        int apid = 100;
        int sid = 0;
        int packetId = 0;
        final int firstParameterStartOffset = 128;
        for(int i = 0; i < 324; ++i) {
            {
                PacketDefinition pd1 = new PacketDefinition();
                pd1.setExternalId(packetId++);
                pd1.setId("TM-325-" + String.format("%04d", apid) + "-" + String.format("%02d", sid));
                pd1.setDescription("Packet " + pd1.getId());
                pd1.setType("TM");
                pd1.getMatchers().add(new IdentFieldMatcher(ifApid, apid));
                pd1.getMatchers().add(new IdentFieldMatcher(ifPType, 3));
                pd1.getMatchers().add(new IdentFieldMatcher(ifPSubtype, 25));
                pd1.getMatchers().add(new IdentFieldMatcher(ifP1, sid));
                pd1.setStructure(new PacketStructure());
                int offset = firstParameterStartOffset;
                for(int k = 0; k < 55; ++k) {
                    ParameterProcessingDefinition toMap = parametersToAdd.remove();
                    offset += addParameter(pd1.getStructure(), toMap, proc2param.get(toMap), offset);
                }
                // Add two common parameters
                ParameterProcessingDefinition c1 = commonPool.get(commonSelector++);
                if(commonSelector >= commonPool.size()) {
                    commonSelector = 0;
                }
                offset += addParameter(pd1.getStructure(), c1, proc2param.get(c1), offset);
                ParameterProcessingDefinition c2 = commonPool.get(commonSelector++);
                if(commonSelector >= commonPool.size()) {
                    commonSelector = 0;
                }
                offset += addParameter(pd1.getStructure(), c2, proc2param.get(c2), offset);
                d.getPacketDefinitions().add(pd1);
            }
            {
                PacketDefinition pd1 = new PacketDefinition();
                pd1.setExternalId(packetId++);
                pd1.setId("TM-325-" + String.format("%04d", apid) + "-" + String.format("%02d", sid));
                pd1.setDescription("Packet " + pd1.getId());
                pd1.setType("TM");
                pd1.getMatchers().add(new IdentFieldMatcher(ifApid, apid));
                pd1.getMatchers().add(new IdentFieldMatcher(ifPType, 3));
                pd1.getMatchers().add(new IdentFieldMatcher(ifPSubtype, 25));
                pd1.getMatchers().add(new IdentFieldMatcher(ifP1, sid));
                pd1.setStructure(new PacketStructure());
                int offset = firstParameterStartOffset;
                for(int k = 0; k < 40; ++k) {
                    ParameterProcessingDefinition toMap = parametersToAdd.remove();
                    offset += addParameter(pd1.getStructure(), toMap, proc2param.get(toMap), offset);
                }
                // Add two common parameters
                ParameterProcessingDefinition c1 = commonPool.get(commonSelector++);
                if(commonSelector >= commonPool.size()) {
                    commonSelector = 0;
                }
                offset += addParameter(pd1.getStructure(), c1, proc2param.get(c1), offset);
                ParameterProcessingDefinition c2 = commonPool.get(commonSelector++);
                if(commonSelector >= commonPool.size()) {
                    commonSelector = 0;
                }
                offset += addParameter(pd1.getStructure(), c2, proc2param.get(c2), offset);
                d.getPacketDefinitions().add(pd1);
            }
            {
                PacketDefinition pd1 = new PacketDefinition();
                pd1.setExternalId(packetId++);
                pd1.setId("TM-325-" + String.format("%04d", apid) + "-" + String.format("%02d", sid));
                pd1.setDescription("Packet " + pd1.getId());
                pd1.setType("TM");
                pd1.getMatchers().add(new IdentFieldMatcher(ifApid, apid));
                pd1.getMatchers().add(new IdentFieldMatcher(ifPType, 3));
                pd1.getMatchers().add(new IdentFieldMatcher(ifPSubtype, 25));
                pd1.getMatchers().add(new IdentFieldMatcher(ifP1, sid));
                pd1.setStructure(new PacketStructure());
                int offset = firstParameterStartOffset;
                for(int k = 0; k < 60; ++k) {
                    ParameterProcessingDefinition toMap = parametersToAdd.remove();
                    offset += addParameter(pd1.getStructure(), toMap, proc2param.get(toMap), offset);
                }
                // Add two common parameters
                ParameterProcessingDefinition c1 = commonPool.get(commonSelector++);
                if(commonSelector >= commonPool.size()) {
                    commonSelector = 0;
                }
                offset += addParameter(pd1.getStructure(), c1, proc2param.get(c1), offset);
                ParameterProcessingDefinition c2 = commonPool.get(commonSelector++);
                if(commonSelector >= commonPool.size()) {
                    commonSelector = 0;
                }
                offset += addParameter(pd1.getStructure(), c2, proc2param.get(c2), offset);
                d.getPacketDefinitions().add(pd1);
            }
            {
                PacketDefinition pd1 = new PacketDefinition();
                pd1.setExternalId(packetId++);
                pd1.setId("TM-325-" + String.format("%04d", apid) + "-" + String.format("%02d", sid));
                pd1.setDescription("Packet " + pd1.getId());
                pd1.setType("TM");
                pd1.getMatchers().add(new IdentFieldMatcher(ifApid, apid));
                pd1.getMatchers().add(new IdentFieldMatcher(ifPType, 3));
                pd1.getMatchers().add(new IdentFieldMatcher(ifPSubtype, 25));
                pd1.getMatchers().add(new IdentFieldMatcher(ifP1, sid));
                pd1.setStructure(new PacketStructure());
                int offset = firstParameterStartOffset;
                for(int k = 0; k < 45; ++k) {
                    ParameterProcessingDefinition toMap = parametersToAdd.remove();
                    offset += addParameter(pd1.getStructure(), toMap, proc2param.get(toMap), offset);
                }
                // Add two common parameters
                ParameterProcessingDefinition c1 = commonPool.get(commonSelector++);
                if(commonSelector >= commonPool.size()) {
                    commonSelector = 0;
                }
                offset += addParameter(pd1.getStructure(), c1, proc2param.get(c1), offset);
                ParameterProcessingDefinition c2 = commonPool.get(commonSelector++);
                if(commonSelector >= commonPool.size()) {
                    commonSelector = 0;
                }
                offset += addParameter(pd1.getStructure(), c2, proc2param.get(c2), offset);
                d.getPacketDefinitions().add(pd1);
            }
            {
                PacketDefinition pd1 = new PacketDefinition();
                pd1.setExternalId(packetId++);
                pd1.setId("TM-325-" + String.format("%04d", apid) + "-" + String.format("%02d", sid));
                pd1.setDescription("Packet " + pd1.getId());
                pd1.setType("TM");
                pd1.getMatchers().add(new IdentFieldMatcher(ifApid, apid));
                pd1.getMatchers().add(new IdentFieldMatcher(ifPType, 3));
                pd1.getMatchers().add(new IdentFieldMatcher(ifPSubtype, 25));
                pd1.getMatchers().add(new IdentFieldMatcher(ifP1, sid));
                pd1.setStructure(new PacketStructure());
                int offset = firstParameterStartOffset;
                for(int k = 0; k < 50; ++k) {
                    ParameterProcessingDefinition toMap = parametersToAdd.remove();
                    offset += addParameter(pd1.getStructure(), toMap, proc2param.get(toMap), offset);
                }
                // Add two common parameters
                ParameterProcessingDefinition c1 = commonPool.get(commonSelector++);
                if(commonSelector >= commonPool.size()) {
                    commonSelector = 0;
                }
                offset += addParameter(pd1.getStructure(), c1, proc2param.get(c1), offset);
                ParameterProcessingDefinition c2 = commonPool.get(commonSelector++);
                if(commonSelector >= commonPool.size()) {
                    commonSelector = 0;
                }
                offset += addParameter(pd1.getStructure(), c2, proc2param.get(c2), offset);
                d.getPacketDefinitions().add(pd1);
            }
            // Increment SID and APID
            if(i % 10 == 9) {
                ++apid;
                sid = 0;
            } else {
                ++sid;
            }
        }
        // Generate packets for PUS 5 events
        // TODO
        // Generate packets for PUS 1 events
        // TODO
        // Generate TC packets for defined activities
        // TODO
        // Generate TC packet for 11,4 command
        // TODO
        return d;
    }

    private static int addParameter(PacketStructure structure, ParameterProcessingDefinition toMap, ParameterDefinition parameterDefinition, int bitOffset) {
        EncodedParameter ep = new EncodedParameter();
        ep.setId("EI-" + extractName(toMap.getLocation()));
        FixedType type = mapType(toMap.getRawType());
        ep.setType(type);
        ep.setLocation(new FixedAbsoluteLocation(bitOffset));
        ep.setLinkedParameter(new FixedLinkedParameter(parameterDefinition));
        ep.setTime(new GenerationTime(null, null, 0));
        structure.getEncodedItems().add(ep);
        switch(type.getType()) {
            case BOOLEAN:
                return 1;
            case ENUMERATED:
            case SIGNED_INTEGER:
            case UNSIGNED_INTEGER:
            case BIT_STRING:
                return type.getLength();
            case REAL:
                if(type.getLength() == 1 || type.getLength() == 3) {
                    return 32;
                } else if(type.getLength() == 2) {
                    return 64;
                } else {
                    return 48;
                }
            case OCTET_STRING:
            case CHARACTER_STRING:
                return type.getLength() * Byte.SIZE;
            case ABSOLUTE_TIME:
            case RELATIVE_TIME: // CUC 4,3 always
                return 7 * Byte.SIZE;
            default:
                throw new IllegalArgumentException("Generation software bug for type " + type.getType());
        }
    }

    private static FixedType mapType(ValueTypeEnum rawType) {
        switch (rawType) {
            case BOOLEAN: return new FixedType(DataTypeEnum.BOOLEAN, 1);
            case ENUMERATED: return new FixedType(DataTypeEnum.ENUMERATED, 16);
            case SIGNED_INTEGER: return new FixedType(DataTypeEnum.SIGNED_INTEGER, 24);
            case UNSIGNED_INTEGER: return new FixedType(DataTypeEnum.UNSIGNED_INTEGER, 32);
            case BIT_STRING: return new FixedType(DataTypeEnum.BIT_STRING, 10);
            case REAL: return new FixedType(DataTypeEnum.REAL, 1);
            case OCTET_STRING: return new FixedType(DataTypeEnum.OCTET_STRING, 12);
            case CHARACTER_STRING: return new FixedType(DataTypeEnum.CHARACTER_STRING, 10);
            case ABSOLUTE_TIME: return new FixedType(DataTypeEnum.ABSOLUTE_TIME, 18);
            case RELATIVE_TIME: return new FixedType(DataTypeEnum.RELATIVE_TIME, 18);
            default:
                throw new IllegalArgumentException("Generation software bug for type " + rawType);
        }
    }

    private static ProcessingDefinition generateProcessingDefinitions() {
        ProcessingDefinition pd = new ProcessingDefinition();
        pd.setParameterDefinitions(new ArrayList<>(90000));
        pd.setEventDefinitions(new ArrayList<>(12000));
        pd.setActivityDefinitions(new ArrayList<>(12000));
        // This function generates a large number of parameters using a specific (hardcoded) pattern, cycled 1000 times.
        // Each cycle generates:
        // - 10 enumeration parameters, used as calibration selector, validity criteria, check criteria
        // - 10 signed integer parameters
        // - 10 unsigned integer parameters
        // - 10 real parameters
        // - 10 string parameters
        // - 10 byte array parameters
        // - 10 bitstring parameters
        // - 10 absolute time parameters
        // - 5 synthetic parameters that perform simple additions/subtraction/conditions from random numerical parameters defined before
        // Therefore 85 parameters are defined per cycle: each parameter will have a prefix of 3 letters (incrementally computed from AAA), the type (P, A or E)
        // and an incremental number

        // Then, as part of the cycle, we have the generation of 10 events with different severity.

        // Then, as part of the cycle, we have the generation of 10 activities with zero, one, two and three arguments.

        // Each cycle generates 85 parameters, 10 events and 10 activities. 1000 cycles are performed: 85K parameters, 10K events, 10k activities.
        Set<String> prefixSet = new TreeSet<>();
        for(int i = 0; i < 1000; ++i) {
            String prefix = generateProcessingPrefix();
            prefixSet.add(prefix);
            generateParameterProcessingData(pd, prefix);
        }

        // Set the counter very high, so that we can map flawlessly TM packets to events, without overlapping with parameter IDs
        ID_COUNTER = 200000;

        for(String prefix : prefixSet) {
            generateEventActivitiesProcessingData(pd, prefix);
        }
        // Finally, we have the generation of one event per PUS 1 verification report (8 events in total), 1 PUS (11,4) activity and 1 activity per type, to change
        // the parameter values
        ID_COUNTER += 100;
        int counter = 0;

        EventProcessingDefinition pus11 = buildEventProcessingDefinition("PUS", counter++, Severity.INFO);
        pus11.setDescription("PUS 1 - Acceptance Success");
        pus11.setType("TC VERIFICATION");
        pus11.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(pus11);

        EventProcessingDefinition pus12 = buildEventProcessingDefinition("PUS", counter++, Severity.ALARM);
        pus12.setDescription("PUS 1 - Acceptance Failure");
        pus12.setType("TC VERIFICATION");
        pus12.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(pus12);

        EventProcessingDefinition pus13 = buildEventProcessingDefinition("PUS", counter++, Severity.INFO);
        pus13.setDescription("PUS 1 - Start Success");
        pus13.setType("TC VERIFICATION");
        pus13.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(pus13);

        EventProcessingDefinition pus14 = buildEventProcessingDefinition("PUS", counter++, Severity.ALARM);
        pus14.setDescription("PUS 1 - Start Failure");
        pus14.setType("TC VERIFICATION");
        pus14.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(pus14);

        EventProcessingDefinition pus15 = buildEventProcessingDefinition("PUS", counter++, Severity.INFO);
        pus15.setDescription("PUS 1 - Progress Success");
        pus15.setType("TC VERIFICATION");
        pus15.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(pus15);

        EventProcessingDefinition pus16 = buildEventProcessingDefinition("PUS", counter++, Severity.ALARM);
        pus16.setDescription("PUS 1 - Progress Success");
        pus16.setType("TC VERIFICATION");
        pus16.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(pus16);

        EventProcessingDefinition pus17 = buildEventProcessingDefinition("PUS", counter++, Severity.INFO);
        pus17.setDescription("PUS 1 - Completion Success");
        pus17.setType("TC VERIFICATION");
        pus17.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(pus17);

        EventProcessingDefinition pus18 = buildEventProcessingDefinition("PUS", counter++, Severity.ALARM);
        pus18.setDescription("PUS 1 - Completion Failure");
        pus18.setType("TC VERIFICATION");
        pus18.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(pus18);

        counter = 0;
        ID_COUNTER += 10;

        ActivityProcessingDefinition apd4 = buildActivityProcessingDefinition("SCHEDULE", counter++);
        apd4.setDescription("Schedule TC execution");
        addActivityArgument(apd4, "Subschedule-ID", ValueTypeEnum.UNSIGNED_INTEGER);
        addActivityArgument(apd4, "Time", ValueTypeEnum.ABSOLUTE_TIME);
        addActivityArgument(apd4, "TC", ValueTypeEnum.OCTET_STRING);
        pd.getActivityDefinitions().add(apd4);

        counter = 0;
        ID_COUNTER += 10;

        ActivityProcessingDefinition seta1 = buildActivityProcessingDefinition("SETTER", counter++);
        addFixedActivityArgument(seta1, "TYPE", ValueTypeEnum.ENUMERATED, String.valueOf(ValueTypeEnum.ENUMERATED.ordinal()));
        addActivityArgument(seta1, "NAME", ValueTypeEnum.CHARACTER_STRING);
        addActivityArgument(seta1, "VALUE", ValueTypeEnum.ENUMERATED);
        pd.getActivityDefinitions().add(seta1);

        ActivityProcessingDefinition seta2 = buildActivityProcessingDefinition("SETTER", counter++);
        addFixedActivityArgument(seta2, "TYPE", ValueTypeEnum.ENUMERATED, String.valueOf(ValueTypeEnum.UNSIGNED_INTEGER.ordinal()));
        addActivityArgument(seta2, "NAME", ValueTypeEnum.CHARACTER_STRING);
        addActivityArgument(seta2, "VALUE", ValueTypeEnum.UNSIGNED_INTEGER);
        pd.getActivityDefinitions().add(seta2);

        ActivityProcessingDefinition seta3 = buildActivityProcessingDefinition("SETTER", counter++);
        addFixedActivityArgument(seta3, "TYPE", ValueTypeEnum.ENUMERATED, String.valueOf(ValueTypeEnum.SIGNED_INTEGER.ordinal()));
        addActivityArgument(seta3, "NAME", ValueTypeEnum.CHARACTER_STRING);
        addActivityArgument(seta3, "VALUE", ValueTypeEnum.SIGNED_INTEGER);
        pd.getActivityDefinitions().add(seta3);

        ActivityProcessingDefinition seta4 = buildActivityProcessingDefinition("SETTER", counter++);
        addFixedActivityArgument(seta4, "TYPE", ValueTypeEnum.ENUMERATED, String.valueOf(ValueTypeEnum.REAL.ordinal()));
        addActivityArgument(seta4, "NAME", ValueTypeEnum.CHARACTER_STRING);
        addActivityArgument(seta4, "VALUE", ValueTypeEnum.REAL);
        pd.getActivityDefinitions().add(seta4);

        ActivityProcessingDefinition seta5 = buildActivityProcessingDefinition("SETTER", counter++);
        addFixedActivityArgument(seta5, "TYPE", ValueTypeEnum.ENUMERATED, String.valueOf(ValueTypeEnum.OCTET_STRING.ordinal()));
        addActivityArgument(seta5, "NAME", ValueTypeEnum.CHARACTER_STRING);
        addActivityArgument(seta5, "VALUE", ValueTypeEnum.OCTET_STRING);
        pd.getActivityDefinitions().add(seta5);

        System.out.println("Generated " + pd.getParameterDefinitions().size() + " parameters");
        System.out.println("Generated " + pd.getEventDefinitions().size() + " events");
        System.out.println("Generated " + pd.getActivityDefinitions().size() + " activities");

        return pd;
    }

    private static void addFixedActivityArgument(ActivityProcessingDefinition seta1, String type, ValueTypeEnum enumerated, String value) {
        PlainArgumentDefinition arg = addActivityArgument(seta1, type, enumerated);
        arg.setFixed(true);
        arg.setDefaultValue(new FixedDefaultValue(DefaultValueType.RAW, value));
    }

    private static String generateProcessingPrefix() {
        String toReturn = String.copyValueOf(PREFIX_COUNTER);
        PREFIX_COUNTER[2] += 1;
        if(PREFIX_COUNTER[2] == 'Z') {
            PREFIX_COUNTER[2] = 'A';
            PREFIX_COUNTER[1] += 1;
        }
        if(PREFIX_COUNTER[1] == 'Z') {
            PREFIX_COUNTER[1] = 'A';
            PREFIX_COUNTER[0] += 1;
        }
        return toReturn;
    }


    private static void generateParameterProcessingData(ProcessingDefinition pd, String prefix) {
        int counter = 0;

        // Enumerations
        ParameterProcessingDefinition pe1 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ENUMERATED, ValueTypeEnum.CHARACTER_STRING);
        pe1.setCalibrations(generateTextualCalibration("ON", "OFF", "UNDETERMINED", "UNKNOWN"));
        pd.getParameterDefinitions().add(pe1);

        ParameterProcessingDefinition pe2 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ENUMERATED, ValueTypeEnum.CHARACTER_STRING);
        pe2.setCalibrations(generateTextualCalibration("ENABLED", "DISABLED", "OVERRIDE", "UNKNOWN"));
        pd.getParameterDefinitions().add(pe2);

        ParameterProcessingDefinition pe3 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ENUMERATED, ValueTypeEnum.CHARACTER_STRING);
        pe3.setCalibrations(generateTextualCalibration("ALARM", "ERROR", "WARNING", "NOMINAL", "UNKNOWN"));
        pd.getParameterDefinitions().add(pe3);

        ParameterProcessingDefinition pe4 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ENUMERATED, ValueTypeEnum.CHARACTER_STRING);
        pe4.setCalibrations(generateTextualCalibration("PRESENT", "ABSENT", "N/A"));
        pe4.setChecks(generateExpectedValueCheck(CheckSeverity.WARNING, ValueTypeEnum.CHARACTER_STRING, "PRESENT"));
        pd.getParameterDefinitions().add(pe4);

        ParameterProcessingDefinition pe5 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ENUMERATED, ValueTypeEnum.ENUMERATED);
        pe5.setChecks(generateExpectedValueCheck(CheckSeverity.ALARM, ValueTypeEnum.ENUMERATED, 12, 13));
        pd.getParameterDefinitions().add(pe5);

        ParameterProcessingDefinition pe6 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ENUMERATED, ValueTypeEnum.ENUMERATED);
        pe6.setChecks(generateExpectedValueCheck(CheckSeverity.WARNING, ValueTypeEnum.ENUMERATED, 0, 1, 2));
        pd.getParameterDefinitions().add(pe6);

        ParameterProcessingDefinition pe7 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ENUMERATED, ValueTypeEnum.ENUMERATED);
        pe7.setValidity(new ValidityCondition(new MatcherDefinition(pe1, MatcherType.EQUAL, ValueTypeEnum.CHARACTER_STRING, "ON"))); // default is to use the engineering value for the comparison
        pe7.setCalibrations(generateTextualCalibration("STATE_A", "STATE_B", "STATE_C", "STATE_D", "STATE_E", "STATE_F", "AUX", "N/A"));
        pe6.setChecks(generateExpectedValueCheck(CheckSeverity.ALARM, ValueTypeEnum.CHARACTER_STRING, "STATE_A", "STATE_B", "STATE_C", "STATE_D", "STATE_E"));
        pd.getParameterDefinitions().add(pe7);

        ParameterProcessingDefinition pe8 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ENUMERATED, ValueTypeEnum.CHARACTER_STRING);
        pe8.setCalibrations(generateTextualCalibration("NOMINAL_A", "NOMINAL_B", "REDUNDANT", "OFF", "N/A"));
        pe8.setChecks(generateExpectedValueCheck(CheckSeverity.WARNING, ValueTypeEnum.CHARACTER_STRING, "NOMINAL_A", "NOMINAL_B"));
        pe8.setChecks(generateExpectedValueCheck(CheckSeverity.ALARM, ValueTypeEnum.CHARACTER_STRING, "NOMINAL_A", "NOMINAL_B", "REDUNDANT"));
        pd.getParameterDefinitions().add(pe8);

        ParameterProcessingDefinition pe9 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ENUMERATED, ValueTypeEnum.CHARACTER_STRING);
        pe9.setCalibrations(generateTextualCalibration("NOMINAL_A", "NOMINAL_B", "REDUNDANT", "OFF", "N/A"));
        pe9.setChecks(generateExpectedValueCheck(CheckSeverity.WARNING, ValueTypeEnum.CHARACTER_STRING, "NOMINAL_A", "NOMINAL_B"));
        pe9.setChecks(generateExpectedValueCheck(CheckSeverity.ALARM, ValueTypeEnum.CHARACTER_STRING, "NOMINAL_A", "NOMINAL_B", "REDUNDANT"));
        pd.getParameterDefinitions().add(pe9);

        ParameterProcessingDefinition pe10 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ENUMERATED, ValueTypeEnum.CHARACTER_STRING);
        List<CalibrationDefinition> ec1 = generateTextualCalibration("ALARM", "ERROR", "WARNING", "NOMINAL", "UNKNOWN");
        ec1.iterator().next().setApplicability(new ValidityCondition(new MatcherDefinition(pe4, MatcherType.EQUAL, ValueTypeEnum.CHARACTER_STRING, "PRESENT")));
        List<CalibrationDefinition> ec2 = generateTextualCalibration("ALARM_ALT", "ERROR_ALT", "WARNING_ALT", "NOMINAL_ALT", "UNKNOWN");
        pe10.setCalibrations(new ArrayList<>(ec1));
        pe10.getCalibrations().addAll(ec2);
        pd.getParameterDefinitions().add(pe10);

        counter += 10;
        ID_COUNTER += 10;

        // Signed integers
        ParameterProcessingDefinition psi1 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.SIGNED_INTEGER, ValueTypeEnum.SIGNED_INTEGER);
        psi1.setUnit("mps");
        psi1.setChecks(generateLimitCheck(CheckSeverity.WARNING, ValueTypeEnum.SIGNED_INTEGER, 100L, 200L));
        pd.getParameterDefinitions().add(psi1);

        ParameterProcessingDefinition psi2 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.SIGNED_INTEGER, ValueTypeEnum.SIGNED_INTEGER);
        psi2.setUnit("mV");
        psi2.setValidity(new ValidityCondition(new MatcherDefinition(pe2, MatcherType.EQUAL, ValueTypeEnum.CHARACTER_STRING, "ENABLED")));
        psi2.setChecks(generateLimitCheck(CheckSeverity.WARNING, ValueTypeEnum.SIGNED_INTEGER, 1500L, 2000L));
        psi2.setChecks(generateLimitCheck(CheckSeverity.ALARM, ValueTypeEnum.SIGNED_INTEGER, 1200L, 2300L));
        pd.getParameterDefinitions().add(psi2);

        ParameterProcessingDefinition psi3 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.SIGNED_INTEGER, ValueTypeEnum.REAL);
        psi3.setUnit("mA");
        psi3.setValidity(new ValidityCondition(new MatcherDefinition(psi2, MatcherType.GT_EQUAL, ValueTypeEnum.SIGNED_INTEGER, "1600")));
        psi3.setCalibrations(generateXYCalibration(new XYCalibrationPoint(2528, 793), new XYCalibrationPoint(3133, 1200), new XYCalibrationPoint(4000, 1000)));
        pd.getParameterDefinitions().add(psi3);

        ParameterProcessingDefinition psi4 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.SIGNED_INTEGER, ValueTypeEnum.REAL);
        psi4.setCalibrations(generatePolyCalibration(5,1,2,0,0,0));
        pd.getParameterDefinitions().add(psi4);

        ParameterProcessingDefinition psi5 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.SIGNED_INTEGER, ValueTypeEnum.REAL);
        psi5.setCalibrations(generateLogCalibration(5,1,2,0,0,0));
        pd.getParameterDefinitions().add(psi5);

        ParameterProcessingDefinition psi6 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.SIGNED_INTEGER, ValueTypeEnum.SIGNED_INTEGER);
        psi6.setValidity(new ValidityCondition(new ExpressionDefinition(extractName(pe1.getLocation()) + " == \"ON\"", Arrays.asList(new SymbolDefinition(extractName(pe1.getLocation()), pe1.getId(), PropertyBinding.ENG_VALUE)))));
        pd.getParameterDefinitions().add(psi6);

        ParameterProcessingDefinition psi7 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.SIGNED_INTEGER, ValueTypeEnum.SIGNED_INTEGER);
        pd.getParameterDefinitions().add(psi7);

        ParameterProcessingDefinition psi8 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.SIGNED_INTEGER, ValueTypeEnum.SIGNED_INTEGER);
        psi8.setUnit("mA");
        pd.getParameterDefinitions().add(psi8);

        ParameterProcessingDefinition psi9 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.SIGNED_INTEGER, ValueTypeEnum.SIGNED_INTEGER);
        pd.getParameterDefinitions().add(psi9);

        ParameterProcessingDefinition psi10 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.SIGNED_INTEGER, ValueTypeEnum.SIGNED_INTEGER);
        pd.getParameterDefinitions().add(psi10);

        counter += 10;
        ID_COUNTER += 10;

        // Unsigned integers
        ParameterProcessingDefinition pui1 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.UNSIGNED_INTEGER, ValueTypeEnum.UNSIGNED_INTEGER);
        pui1.setUnit("mps");
        pui1.setChecks(generateLimitCheck(CheckSeverity.WARNING, ValueTypeEnum.SIGNED_INTEGER, 100L, 200L));
        pd.getParameterDefinitions().add(pui1);

        ParameterProcessingDefinition pui2 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.UNSIGNED_INTEGER, ValueTypeEnum.UNSIGNED_INTEGER);
        pui2.setUnit("mV");
        pui2.setValidity(new ValidityCondition(new MatcherDefinition(pe5, MatcherType.EQUAL, ValueTypeEnum.ENUMERATED, "13")));
        pui2.setChecks(generateLimitCheck(CheckSeverity.WARNING, ValueTypeEnum.UNSIGNED_INTEGER, 1500L, 2000L));
        pui2.setChecks(generateLimitCheck(CheckSeverity.ALARM, ValueTypeEnum.UNSIGNED_INTEGER, 1200L, 2300L));
        pd.getParameterDefinitions().add(pui2);

        ParameterProcessingDefinition pui3 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.UNSIGNED_INTEGER, ValueTypeEnum.REAL);
        pui3.setUnit("mA");
        pui3.setValidity(new ValidityCondition(new MatcherDefinition(pui2, MatcherType.GT_EQUAL, ValueTypeEnum.UNSIGNED_INTEGER, "1600")));
        pui3.setCalibrations(generateXYCalibration(new XYCalibrationPoint(2528, 793), new XYCalibrationPoint(3133, 1200), new XYCalibrationPoint(4000, 1000)));
        pd.getParameterDefinitions().add(pui3);

        ParameterProcessingDefinition pui4 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.UNSIGNED_INTEGER, ValueTypeEnum.REAL);
        pui4.setCalibrations(generatePolyCalibration(5,1,2,0,0,0));
        pd.getParameterDefinitions().add(pui4);

        ParameterProcessingDefinition pui5 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.UNSIGNED_INTEGER, ValueTypeEnum.REAL);
        pui5.setCalibrations(generateLogCalibration(5,1,2,0,0,0));
        pd.getParameterDefinitions().add(pui5);

        ParameterProcessingDefinition pui6 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.UNSIGNED_INTEGER, ValueTypeEnum.UNSIGNED_INTEGER);
        pui6.setValidity(new ValidityCondition(new ExpressionDefinition(extractName(pe1.getLocation()) + " == \"ON\"", Arrays.asList(new SymbolDefinition(extractName(pe1.getLocation()), pe1.getId(), PropertyBinding.ENG_VALUE)))));
        pd.getParameterDefinitions().add(pui6);

        ParameterProcessingDefinition pui7 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.UNSIGNED_INTEGER, ValueTypeEnum.UNSIGNED_INTEGER);
        pd.getParameterDefinitions().add(pui7);

        ParameterProcessingDefinition pui8 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.UNSIGNED_INTEGER, ValueTypeEnum.UNSIGNED_INTEGER);
        pui8.setUnit("mA");
        pd.getParameterDefinitions().add(pui8);

        ParameterProcessingDefinition pui9 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.UNSIGNED_INTEGER, ValueTypeEnum.UNSIGNED_INTEGER);
        pd.getParameterDefinitions().add(pui9);

        ParameterProcessingDefinition pui10 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.UNSIGNED_INTEGER, ValueTypeEnum.UNSIGNED_INTEGER);
        pd.getParameterDefinitions().add(pui10);

        counter += 10;
        ID_COUNTER += 10;

        // Reals
        ParameterProcessingDefinition pr1 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.REAL, ValueTypeEnum.REAL);
        pr1.setUnit("mps");
        pr1.setChecks(generateLimitCheck(CheckSeverity.WARNING, ValueTypeEnum.SIGNED_INTEGER, 20, 30));
        pd.getParameterDefinitions().add(pr1);

        ParameterProcessingDefinition pr2 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.REAL, ValueTypeEnum.REAL);
        pr2.setUnit("mV");
        pr2.setValidity(new ValidityCondition(new MatcherDefinition(pe5, MatcherType.EQUAL, ValueTypeEnum.ENUMERATED, "13")));
        pr2.setChecks(new ArrayList<>());
        pr2.getChecks().addAll(generateLimitCheck(CheckSeverity.WARNING, ValueTypeEnum.REAL, 1300, 1500));
        pr2.getChecks().addAll(generateLimitCheck(CheckSeverity.ALARM, ValueTypeEnum.REAL, 1000, 2000));
        pd.getParameterDefinitions().add(pr2);

        ParameterProcessingDefinition pr3 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.REAL, ValueTypeEnum.REAL);
        pr3.setUnit("mA");
        pr3.setValidity(new ValidityCondition(new MatcherDefinition(pr2, MatcherType.GT_EQUAL, ValueTypeEnum.REAL, "1350.0")));
        pr3.setCalibrations(generateXYCalibration(new XYCalibrationPoint(2528, 793), new XYCalibrationPoint(3133, 1200), new XYCalibrationPoint(4000, 1000)));
        pd.getParameterDefinitions().add(pr3);

        ParameterProcessingDefinition pr4 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.REAL, ValueTypeEnum.REAL);
        pr4.setCalibrations(generatePolyCalibration(5,1,2,0,0,0));
        pd.getParameterDefinitions().add(pr4);

        ParameterProcessingDefinition pr5 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.REAL, ValueTypeEnum.REAL);
        pr5.setCalibrations(generateLogCalibration(5,1,2,0,0,0));
        pd.getParameterDefinitions().add(pr5);

        ParameterProcessingDefinition pr6 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.REAL, ValueTypeEnum.REAL);
        pr6.setValidity(new ValidityCondition(new ExpressionDefinition(extractName(pe1.getLocation()) + " == \"ON\"", Arrays.asList(new SymbolDefinition(extractName(pe1.getLocation()), pe1.getId(), PropertyBinding.ENG_VALUE)))));
        pd.getParameterDefinitions().add(pr6);

        ParameterProcessingDefinition pr7 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.REAL, ValueTypeEnum.REAL);
        pd.getParameterDefinitions().add(pr7);

        ParameterProcessingDefinition pr8 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.REAL, ValueTypeEnum.REAL);
        pr8.setUnit("mA");
        pd.getParameterDefinitions().add(pr8);

        ParameterProcessingDefinition pr9 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.REAL, ValueTypeEnum.REAL);
        pr9.setChecks(new ArrayList<>());
        List<CheckDefinition> cks1 = generateLimitCheck(CheckSeverity.WARNING, ValueTypeEnum.REAL, 1300, 1500);
        cks1.iterator().next().setApplicability(new ValidityCondition(new MatcherDefinition(pe6, MatcherType.GT_EQUAL, ValueTypeEnum.ENUMERATED, "2")));
        List<CheckDefinition> cks2 = generateLimitCheck(CheckSeverity.ALARM, ValueTypeEnum.REAL, 1000, 1800);
        cks2.iterator().next().setApplicability(new ValidityCondition(new MatcherDefinition(pe6, MatcherType.GT_EQUAL, ValueTypeEnum.ENUMERATED, "2")));
        pr9.getChecks().addAll(cks1);
        pr9.getChecks().addAll(cks2);
        pd.getParameterDefinitions().add(pr9);

        ParameterProcessingDefinition pr10 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.REAL, ValueTypeEnum.REAL);
        pr10.setCalibrations(Collections.singletonList(new ExpressionCalibration(new ExpressionDefinition("if (input > 20.0) 1; else 0;", new ArrayList<>()))));
        pd.getParameterDefinitions().add(pr10);

        counter += 10;
        ID_COUNTER += 10;

        // Strings
        ParameterProcessingDefinition pstr1 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.CHARACTER_STRING, ValueTypeEnum.CHARACTER_STRING);
        pstr1.setChecks(generateExpectedValueCheck(CheckSeverity.WARNING, ValueTypeEnum.CHARACTER_STRING, "TEST1", "TEST2"));
        pd.getParameterDefinitions().add(pstr1);

        ParameterProcessingDefinition pstr2 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.CHARACTER_STRING, ValueTypeEnum.CHARACTER_STRING);
        pstr2.setValidity(new ValidityCondition(new MatcherDefinition(pe2, MatcherType.EQUAL, ValueTypeEnum.CHARACTER_STRING, "ENABLED")));
        pd.getParameterDefinitions().add(pstr2);

        ParameterProcessingDefinition pstr3 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.CHARACTER_STRING, ValueTypeEnum.CHARACTER_STRING);
        pd.getParameterDefinitions().add(pstr3);

        ParameterProcessingDefinition pstr4 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.CHARACTER_STRING, ValueTypeEnum.CHARACTER_STRING);
        pd.getParameterDefinitions().add(pstr4);

        ParameterProcessingDefinition pstr5 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.CHARACTER_STRING, ValueTypeEnum.CHARACTER_STRING);
        pd.getParameterDefinitions().add(pstr5);

        ParameterProcessingDefinition pstr6 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.CHARACTER_STRING, ValueTypeEnum.CHARACTER_STRING);
        pstr6.setValidity(new ValidityCondition(new ExpressionDefinition(extractName(pe1.getLocation()) + " == \"ON\"", Arrays.asList(new SymbolDefinition(extractName(pe1.getLocation()), pe1.getId(), PropertyBinding.ENG_VALUE)))));
        pd.getParameterDefinitions().add(pstr6);

        ParameterProcessingDefinition pstr7 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.CHARACTER_STRING, ValueTypeEnum.CHARACTER_STRING);
        pd.getParameterDefinitions().add(pstr7);

        ParameterProcessingDefinition pstr8 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.CHARACTER_STRING, ValueTypeEnum.CHARACTER_STRING);
        pd.getParameterDefinitions().add(pstr8);

        ParameterProcessingDefinition pstr9 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.CHARACTER_STRING, ValueTypeEnum.CHARACTER_STRING);
        pd.getParameterDefinitions().add(pstr9);

        ParameterProcessingDefinition pstr10 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.CHARACTER_STRING, ValueTypeEnum.CHARACTER_STRING);
        pd.getParameterDefinitions().add(pstr10);

        counter += 10;
        ID_COUNTER += 10;

        // Octet strings
        ParameterProcessingDefinition pocts1 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.OCTET_STRING, ValueTypeEnum.OCTET_STRING);
        pocts1.setChecks(generateExpectedValueCheck(CheckSeverity.WARNING, ValueTypeEnum.OCTET_STRING, "00001100", "00110011"));
        pd.getParameterDefinitions().add(pocts1);

        ParameterProcessingDefinition pocts2 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.OCTET_STRING, ValueTypeEnum.OCTET_STRING);
        pocts2.setValidity(new ValidityCondition(new MatcherDefinition(pe2, MatcherType.EQUAL, ValueTypeEnum.CHARACTER_STRING, "ENABLED")));
        pd.getParameterDefinitions().add(pocts2);

        ParameterProcessingDefinition pocts3 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.OCTET_STRING, ValueTypeEnum.OCTET_STRING);
        pd.getParameterDefinitions().add(pocts3);

        ParameterProcessingDefinition pocts4 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.OCTET_STRING, ValueTypeEnum.OCTET_STRING);
        pd.getParameterDefinitions().add(pocts4);

        ParameterProcessingDefinition pocts5 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.OCTET_STRING, ValueTypeEnum.OCTET_STRING);
        pd.getParameterDefinitions().add(pocts5);

        ParameterProcessingDefinition pocts6 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.OCTET_STRING, ValueTypeEnum.OCTET_STRING);
        pocts6.setValidity(new ValidityCondition(new ExpressionDefinition(extractName(pe1.getLocation()) + " == \"ON\"", Arrays.asList(new SymbolDefinition(extractName(pe1.getLocation()), pe1.getId(), PropertyBinding.ENG_VALUE)))));
        pd.getParameterDefinitions().add(pocts6);

        ParameterProcessingDefinition pocts7 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.OCTET_STRING, ValueTypeEnum.OCTET_STRING);
        pd.getParameterDefinitions().add(pocts7);

        ParameterProcessingDefinition pocts8 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.OCTET_STRING, ValueTypeEnum.OCTET_STRING);
        pd.getParameterDefinitions().add(pocts8);

        ParameterProcessingDefinition pocts9 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.OCTET_STRING, ValueTypeEnum.OCTET_STRING);
        pd.getParameterDefinitions().add(pocts9);

        ParameterProcessingDefinition pocts10 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.OCTET_STRING, ValueTypeEnum.OCTET_STRING);
        pd.getParameterDefinitions().add(pocts10);

        counter += 10;
        ID_COUNTER += 10;

        // Bit strings
        ParameterProcessingDefinition pbits1 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.BIT_STRING, ValueTypeEnum.BIT_STRING);
        pbits1.setChecks(generateExpectedValueCheck(CheckSeverity.WARNING, ValueTypeEnum.BIT_STRING, "_01001010", "_01001011"));
        pd.getParameterDefinitions().add(pbits1);

        ParameterProcessingDefinition pbits2 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.BIT_STRING, ValueTypeEnum.BIT_STRING);
        pbits2.setValidity(new ValidityCondition(new MatcherDefinition(pe2, MatcherType.EQUAL, ValueTypeEnum.CHARACTER_STRING, "ENABLED")));
        pd.getParameterDefinitions().add(pbits2);

        ParameterProcessingDefinition pbits3 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.BIT_STRING, ValueTypeEnum.BIT_STRING);
        pd.getParameterDefinitions().add(pbits3);

        ParameterProcessingDefinition pbits4 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.BIT_STRING, ValueTypeEnum.BIT_STRING);
        pd.getParameterDefinitions().add(pbits4);

        ParameterProcessingDefinition pbits5 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.BIT_STRING, ValueTypeEnum.BIT_STRING);
        pd.getParameterDefinitions().add(pbits5);

        ParameterProcessingDefinition pbits6 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.BIT_STRING, ValueTypeEnum.BIT_STRING);
        pbits6.setValidity(new ValidityCondition(new ExpressionDefinition(extractName(pe1.getLocation()) + " == \"ON\"", Arrays.asList(new SymbolDefinition(extractName(pe1.getLocation()), pe1.getId(), PropertyBinding.ENG_VALUE)))));
        pd.getParameterDefinitions().add(pbits6);

        ParameterProcessingDefinition pbits7 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.BIT_STRING, ValueTypeEnum.BIT_STRING);
        pd.getParameterDefinitions().add(pbits7);

        ParameterProcessingDefinition pbits8 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.BIT_STRING, ValueTypeEnum.BIT_STRING);
        pd.getParameterDefinitions().add(pbits8);

        ParameterProcessingDefinition pbits9 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.BIT_STRING, ValueTypeEnum.BIT_STRING);
        pd.getParameterDefinitions().add(pbits9);

        ParameterProcessingDefinition pbits10 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.BIT_STRING, ValueTypeEnum.BIT_STRING);
        pd.getParameterDefinitions().add(pbits10);

        counter += 10;
        ID_COUNTER += 10;

        // Absolute time
        ParameterProcessingDefinition pabt1 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ABSOLUTE_TIME, ValueTypeEnum.ABSOLUTE_TIME);
        pd.getParameterDefinitions().add(pabt1);

        ParameterProcessingDefinition pabt2 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ABSOLUTE_TIME, ValueTypeEnum.ABSOLUTE_TIME);
        pabt2.setValidity(new ValidityCondition(new MatcherDefinition(pe2, MatcherType.EQUAL, ValueTypeEnum.CHARACTER_STRING, "ENABLED")));
        pd.getParameterDefinitions().add(pabt2);

        ParameterProcessingDefinition pabt3 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ABSOLUTE_TIME, ValueTypeEnum.ABSOLUTE_TIME);
        pd.getParameterDefinitions().add(pabt3);

        ParameterProcessingDefinition pabt4 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ABSOLUTE_TIME, ValueTypeEnum.ABSOLUTE_TIME);
        pd.getParameterDefinitions().add(pabt4);

        ParameterProcessingDefinition pabt5 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ABSOLUTE_TIME, ValueTypeEnum.ABSOLUTE_TIME);
        pd.getParameterDefinitions().add(pabt5);

        ParameterProcessingDefinition pabt6 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ABSOLUTE_TIME, ValueTypeEnum.ABSOLUTE_TIME);
        pabt6.setValidity(new ValidityCondition(new ExpressionDefinition(extractName(pe1.getLocation()) + " == \"ON\"", Arrays.asList(new SymbolDefinition(extractName(pe1.getLocation()), pe1.getId(), PropertyBinding.ENG_VALUE)))));
        pd.getParameterDefinitions().add(pabt6);

        ParameterProcessingDefinition pabt7 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ABSOLUTE_TIME, ValueTypeEnum.ABSOLUTE_TIME);
        pd.getParameterDefinitions().add(pabt7);

        ParameterProcessingDefinition pabt8 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ABSOLUTE_TIME, ValueTypeEnum.ABSOLUTE_TIME);
        pd.getParameterDefinitions().add(pabt8);

        ParameterProcessingDefinition pabt9 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ABSOLUTE_TIME, ValueTypeEnum.ABSOLUTE_TIME);
        pd.getParameterDefinitions().add(pabt9);

        ParameterProcessingDefinition pabt10 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ABSOLUTE_TIME, ValueTypeEnum.ABSOLUTE_TIME);
        pd.getParameterDefinitions().add(pabt10);

        counter += 10;
        ID_COUNTER += 10;

        // Synthetic
        ParameterProcessingDefinition ps1 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ENUMERATED, ValueTypeEnum.CHARACTER_STRING);
        ps1.setExpression(new ExpressionDefinition("function eval() { \n" +
                "  if(" + extractName(pe4.getLocation()) + " == \"PRESENT\") return 0; else return 1; \n" +
                "} \n" +
                "eval();", Arrays.asList(
                new SymbolDefinition(extractName(pe4.getLocation()), pe4.getId(), PropertyBinding.ENG_VALUE)
        )));
        ps1.setCalibrations(generateTextualCalibration("PRESENT_SET", "PRESENT_NOT_SET"));
        pd.getParameterDefinitions().add(ps1);

        ParameterProcessingDefinition ps2 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.REAL, ValueTypeEnum.REAL);
        ps2.setExpression(new ExpressionDefinition("function eval() { \n" +
                "  if(" + extractName(pe4.getLocation()) + " == \"PRESENT\") return " + extractName(pr4.getLocation()) + "; else return " + extractName(pr4.getLocation())
                + " + " + extractName(pr5.getLocation()) + "; \n" +
                "} \n" +
                "eval();", Arrays.asList(
                new SymbolDefinition(extractName(pe4.getLocation()), pe4.getId(), PropertyBinding.ENG_VALUE),
                new SymbolDefinition(extractName(pr4.getLocation()), pr4.getId(), PropertyBinding.ENG_VALUE),
                new SymbolDefinition(extractName(pr5.getLocation()), pr5.getId(), PropertyBinding.ENG_VALUE)
        )));
        pd.getParameterDefinitions().add(ps2);

        ParameterProcessingDefinition ps3 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.REAL, ValueTypeEnum.REAL);
        ps3.setExpression(new ExpressionDefinition("function eval() { \n" +
                "  if(" + extractName(pr3.getLocation()) + " < 1400.0) return " + extractName(pr4.getLocation()) + "; else return " + extractName(pr4.getLocation())
                + " + " + extractName(pr5.getLocation()) + "; \n" +
                "} \n" +
                "eval();", Arrays.asList(
                new SymbolDefinition(extractName(pr3.getLocation()), pr3.getId(), PropertyBinding.ENG_VALUE),
                new SymbolDefinition(extractName(pr4.getLocation()), pr4.getId(), PropertyBinding.ENG_VALUE),
                new SymbolDefinition(extractName(pr5.getLocation()), pr5.getId(), PropertyBinding.ENG_VALUE)
        )));
        pd.getParameterDefinitions().add(ps3);

        ParameterProcessingDefinition ps4 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.ENUMERATED, ValueTypeEnum.CHARACTER_STRING);
        ps4.setExpression(new ExpressionDefinition("function eval() { \n" +
                "  if(" + extractName(pe4.getLocation()) + " != \"PRESENT\") return 2; else if(" + extractName(pui9.getLocation()) + " != 10) return 1; else return 0;\n" +
                "} \n" +
                "eval();", Arrays.asList(
                new SymbolDefinition(extractName(pe4.getLocation()), pe4.getId(), PropertyBinding.ENG_VALUE),
                new SymbolDefinition(extractName(pui9.getLocation()), pui9.getId(), PropertyBinding.ENG_VALUE)
        )));
        ps4.setCalibrations(generateTextualCalibration("A", "B", "C"));
        pd.getParameterDefinitions().add(ps4);

        // Synthetic
        ParameterProcessingDefinition ps5 = buildParameterProcessingDefinition(prefix, counter++, ValueTypeEnum.CHARACTER_STRING, ValueTypeEnum.CHARACTER_STRING);
        ps1.setExpression(new ExpressionDefinition("function eval() { \n" +
                "  return " + extractName(pe4.getLocation()) + " + \"_TEST\" \n" +
                "} \n" +
                "eval();", Arrays.asList(
                new SymbolDefinition(extractName(pe4.getLocation()), pe4.getId(), PropertyBinding.SOURCE_VALUE)
        )));
        pd.getParameterDefinitions().add(ps5);
    }


    private static void generateEventActivitiesProcessingData(ProcessingDefinition pd, String prefix) {
        int counter = 0;

        // Events: 10 onboard, 2 condition-driven
        EventProcessingDefinition epd1 = buildEventProcessingDefinition(prefix, counter++, Severity.ALARM);
        epd1.setType("ON-BOARD");
        epd1.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(epd1);

        EventProcessingDefinition epd2 = buildEventProcessingDefinition(prefix, counter++, Severity.WARN);
        epd2.setType("ON-BOARD");
        epd2.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(epd2);

        EventProcessingDefinition epd3 = buildEventProcessingDefinition(prefix, counter++, Severity.ERROR);
        epd3.setType("ON-BOARD");
        epd3.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(epd3);

        EventProcessingDefinition epd4 = buildEventProcessingDefinition(prefix, counter++, Severity.INFO);
        epd4.setType("ON-BOARD");
        epd4.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(epd4);

        EventProcessingDefinition epd5 = buildEventProcessingDefinition(prefix, counter++, Severity.ALARM);
        epd5.setType("ON-BOARD");
        epd5.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(epd5);

        EventProcessingDefinition epd6 = buildEventProcessingDefinition(prefix, counter++, Severity.WARN);
        epd6.setType("ON-BOARD");
        epd6.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(epd6);

        EventProcessingDefinition epd7 = buildEventProcessingDefinition(prefix, counter++, Severity.ERROR);
        epd7.setType("ON-BOARD");
        epd7.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(epd7);

        EventProcessingDefinition epd8 = buildEventProcessingDefinition(prefix, counter++, Severity.INFO);
        epd8.setType("ON-BOARD");
        epd8.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(epd8);

        EventProcessingDefinition epd9 = buildEventProcessingDefinition(prefix, counter++, Severity.ALARM);
        epd9.setType("ON-BOARD");
        epd9.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(epd9);

        EventProcessingDefinition epd10 = buildEventProcessingDefinition(prefix, counter++, Severity.WARN);
        epd10.setType("ON-BOARD");
        epd10.setInhibitionPeriod(0);
        pd.getEventDefinitions().add(epd10);

        counter = 0;
        ID_COUNTER += 100;

        // Activities
        for(int i = 0; i < 2; ++i) {
            ActivityProcessingDefinition apd1 = buildActivityProcessingDefinition(prefix, counter++);
            pd.getActivityDefinitions().add(apd1);

            ActivityProcessingDefinition apd2 = buildActivityProcessingDefinition(prefix, counter++);
            addActivityArgument(apd2, "ARG1", ValueTypeEnum.ENUMERATED);
            pd.getActivityDefinitions().add(apd2);

            ActivityProcessingDefinition apd3 = buildActivityProcessingDefinition(prefix, counter++);
            addActivityArgument(apd3, "ARG1", ValueTypeEnum.ENUMERATED);
            addActivityArgument(apd3, "ARG2", ValueTypeEnum.SIGNED_INTEGER);
            pd.getActivityDefinitions().add(apd3);

            ActivityProcessingDefinition apd4 = buildActivityProcessingDefinition(prefix, counter++);
            addActivityArgument(apd4, "ARG1", ValueTypeEnum.UNSIGNED_INTEGER);
            addActivityArgument(apd4, "ARG2", ValueTypeEnum.REAL);
            addActivityArgument(apd4, "ARG3", ValueTypeEnum.CHARACTER_STRING);
            pd.getActivityDefinitions().add(apd4);

            ActivityProcessingDefinition apd5 = buildActivityProcessingDefinition(prefix, counter++);
            addActivityArgument(apd5, "ARG1", ValueTypeEnum.BOOLEAN);
            addActivityArgument(apd5, "ARG2", ValueTypeEnum.ABSOLUTE_TIME);
            addActivityArgument(apd5, "ARG3", ValueTypeEnum.OCTET_STRING);
            pd.getActivityDefinitions().add(apd5);
        }

        ID_COUNTER += 100;
    }

    private static PlainArgumentDefinition addActivityArgument(ActivityProcessingDefinition activity, String name, ValueTypeEnum type) {
        PlainArgumentDefinition def = new PlainArgumentDefinition(name, type, type, null, false, null, null, null);
        activity.getArguments().add(def);
        return def;
    }

    private static ActivityProcessingDefinition buildActivityProcessingDefinition(String prefix, int counter) {
        ActivityProcessingDefinition epd = new ActivityProcessingDefinition();
        epd.setId(ID_COUNTER++);
        String name = String.format("%s%s%04d", prefix, "E", ++counter);
        epd.setDescription("Description of activity " + name);
        epd.setLocation(String.format("SPACE.SC.%s.%s", prefix, name));
        epd.setType("TC");
        epd.setArguments(new ArrayList<>());
        return epd;
    }

    private static EventProcessingDefinition buildEventProcessingDefinition(String prefix, int counter, Severity severity) {
        EventProcessingDefinition epd = new EventProcessingDefinition();
        epd.setId(ID_COUNTER++);
        String name = String.format("%s%s%04d", prefix, "E", ++counter);
        epd.setDescription("Description of event " + name + " with severity " + severity);
        epd.setLocation(String.format("SPACE.SC.%s.%s", prefix, name));
        return epd;
    }

    private static String extractName(String location) {
        return location.substring(location.lastIndexOf('.') + 1);
    }

    private static List<CalibrationDefinition> generateLogCalibration(int a0, int a1, int a2, int a3, int a4, int a5) {
        LogCalibration pc = new LogCalibration(a0, a1, a2, a3, a4, a5);
        return Collections.singletonList(pc);
    }

    private static List<CalibrationDefinition> generatePolyCalibration(int a0, int a1, int a2, int a3, int a4, int a5) {
        PolyCalibration pc = new PolyCalibration(a0, a1, a2, a3, a4, a5);
        return Collections.singletonList(pc);
    }

    private static List<CalibrationDefinition> generateXYCalibration(XYCalibrationPoint... points) {
        XYCalibration cl = new XYCalibration(Arrays.asList(points), true);
        return Collections.singletonList(cl);
    }

    private static List<CheckDefinition> generateLimitCheck(CheckSeverity severity, ValueTypeEnum type, Object min, Object max) {
        LimitCheck lc = new LimitCheck("LIM_CK", severity, 2, type, min != null ? min.toString() : null, max != null ? max.toString() : null);
        return Collections.singletonList(lc);
    }

    private static List<CheckDefinition> generateExpectedValueCheck(CheckSeverity severity, ValueTypeEnum type, Object... values ) {
        ExpectedCheck ec = new ExpectedCheck("EXP_CK", severity, 1, type, Stream.of(values).map(String::valueOf).collect(Collectors.toList()));
        return Collections.singletonList(ec);
    }

    private static ParameterProcessingDefinition buildParameterProcessingDefinition(String prefix, int counter, ValueTypeEnum rawType, ValueTypeEnum engType) {
        ParameterProcessingDefinition pe1 = new ParameterProcessingDefinition();
        pe1.setId(ID_COUNTER++);
        pe1.setRawType(rawType);
        pe1.setUnit(null);
        String name = String.format("%s%s%04d", prefix, "P", ++counter);
        pe1.setDescription("Description of parameter " + name + " of raw type " + rawType);
        pe1.setLocation(String.format("SPACE.SC.%s.%s", prefix, name));
        pe1.setEngineeringType(engType);
        return pe1;
    }

    private static List<CalibrationDefinition> generateTextualCalibration(String... values) {
        EnumCalibration cd = new EnumCalibration();
        cd.setApplicability(null);
        cd.setDefaultValue(values[values.length - 1]);
        cd.setPoints(new ArrayList<>());
        for(int i = 0; i < values.length - 1; ++i) {
            cd.getPoints().add(new EnumCalibrationPoint(i, values[i]));
        }
        return Collections.singletonList(cd);
    }

    private static void storeDefinitions(ProcessingDefinition defs, String folder) throws IOException {
        File destination = new File(folder + File.separator + "model.xml");
        if(!destination.exists()) {
            destination.createNewFile();
        }
        ProcessingDefinition.save(defs, new FileOutputStream(destination));
    }

    private static void storePacketDefinitions(Definition packetDefs, String folder) throws IOException {
        File destination = new File(folder + File.separator + "tmtc.xml");
        if(!destination.exists()) {
            destination.createNewFile();
        }
        Definition.save(packetDefs, new FileOutputStream(destination));
    }
}
