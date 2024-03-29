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

package eu.dariolucia.reatmetric.driver.spacecraft.common;

public class Constants {
    // RawData Names
    public static final String N_TM_TRANSFER_FRAME = "TRANSFER FRAME";
    public static final String N_UNKNOWN_PACKET = "UNKNOWN";
    public static final String N_UNKNOWN_VCA = "UNKNOWN";
    public static final String N_IDLE_PACKET = "IDLE PACKET";
    public static final String N_TIME_COEFFICIENTS = "TIME COEFFICIENT";
    public static final String N_TC_VERIFICATION_MAP = "TC VERIFICATION MAP";
    public static final String N_SCHEDULE_MODEL_STATE = "ONBOARD SCHEDULE";

    // RawData Types
    public static final String T_TM_FRAME = "TM FRAME";
    public static final String T_AOS_FRAME = "AOS FRAME";
    public static final String T_TM_PACKET = "TM PACKET";
    public static final String T_TM_VCA = "TM VCA";
    public static final String T_IDLE_PACKET = "IDLE PACKET";
    public static final String T_TC_PACKET = "TC PACKET";
    public static final String T_TC_VCA = "TC VCA";
    public static final String T_BAD_TM = "BAD TM";
    public static final String T_BAD_PACKET = "BAD PACKET";
    public static final String T_TIME_COEFFICIENTS = "TIME COEFF";
    public static final String T_TC_VERIFICATION_MAP = "TC VERIFICATION MAP";
    public static final String T_SCHEDULE_MODEL_STATE = "ONBOARD SCHEDULE";

    // Annotations at AbstractTransferFrame/SpacePacket level
    public static final String ANNOTATION_ROUTE = "##ROUTE";
    public static final String ANNOTATION_SOURCE = "##SOURCE";
    public static final String ANNOTATION_GEN_TIME = "##GENTIME";
    public static final String ANNOTATION_RCP_TIME = "##RCPTIME";
    public static final String ANNOTATION_TM_PUS_HEADER = "##TM_PUS_HEADER";
    public static final String ANNOTATION_UNIQUE_ID = "##UNIQUE_ID";
    public static final String ANNOTATION_VCID = "##VCID";
    public static final String ANNOTATION_TC_TRACKER = "##TC_TRACKER";
    public static final String ANNOTATION_TC_PUS_HEADER = "##TC_PUS_HEADER";
    public static final String ANNOTATION_TC_FRAME_ID = "##TC_FRAME_TRACKING_ID";

    // encdec definition: TM/TC packet type
    public static final String ENCDEC_TM_PACKET_TYPE = "TM";
    public static final String ENCDEC_TC_PACKET_TYPE = "TC";

    // Processing model: event types
    public static final String EVT_T_ONBOARD_EVENT = "ONBOARD EVENT";
    public static final String EVT_T_TC_VERIFICATION_EVENT = "TC VERIFICATION";

    // Activity invocation properties
    public static final String ACTIVITY_PROPERTY_OVERRIDE_ACK = "pus-ack-override";
    public static final String ACTIVITY_PROPERTY_OVERRIDE_SOURCE_ID = "pus-source-override";
    public static final String ACTIVITY_PROPERTY_OVERRIDE_MAP_ID = "map-id-override";
    public static final String ACTIVITY_PROPERTY_SCHEDULED_TIME = "tc-scheduled-time";
    public static final String ACTIVITY_PROPERTY_OVERRIDE_TCVC_ID = "tc-vc-id-override";
    public static final String ACTIVITY_PROPERTY_OVERRIDE_USE_AD_FRAME = "use-ad-mode-override";
    public static final String ACTIVITY_PROPERTY_TC_GROUP_NAME = "group-tc-name";
    public static final String ACTIVITY_PROPERTY_TC_GROUP_TRANSMIT = "group-tc-transmit";
    public static final String ACTIVITY_PROPERTY_SUBSCHEDULE_ID = "onboard-sub-schedule-id";
    public static final String ACTIVITY_PROPERTY_SUBSCHEDULE_TRACKING_ID = "linked-scheduled-activity-occurrence";

    // Activity stages

    public static final String STAGE_ENDPOINT_RECEPTION = "Endpoint Reception";
    public static final String STAGE_GROUND_STATION_RECEPTION = "Ground Station Reception";
    public static final String STAGE_GROUND_STATION_UPLINK = "Ground Station Uplink";
    public static final String STAGE_GROUND_STATION_EXECUTION = "Ground Station Execution"; // for THROW-EVENT activities
    public static final String STAGE_ONBOARD_RECEPTION = "On-board Reception";
    public static final String STAGE_SPACECRAFT_SCHEDULED = "Scheduled";
    public static final String STAGE_ONBOARD_AVAILABILITY = "On-board Availability";
    public static final String STAGE_SPACECRAFT_ACCEPTED = "On-board Acceptance";
    public static final String STAGE_SPACECRAFT_STARTED = "On-board Start";
    public static final String STAGE_SPACECRAFT_PROGRESS = "On-board Progress";
    public static final String STAGE_SPACECRAFT_COMPLETED = "On-board Completion";

    public static final String STAGE_FOP_DIRECTIVE = "FOP Directive Execution";
    public static final String STAGE_LOCAL_EXECUTION = "Local Execution";

    // CLTU Throw Event
    public static final String SLE_CLTU_THROW_EVENT_ACTIVITY_TYPE = "THROW-EVENT";
    public static final String SLE_CLTU_THROW_EVENT_IDENTIFIER_ARG_NAME = "EVENT-IDENTIFIER";
    public static final String SLE_CLTU_THROW_EVENT_QUALIFIER_ARG_NAME = "EVENT-QUALIFIER";

    // COP-1
    public static final String TC_COP1_ACTIVITY_TYPE = "COP-1";
    public static final String TC_COP1_DIRECTIVE_NAME = "DIRECTIVE";
    public static final String TC_COP1_SET_AD_NAME = "SET_AD";

    // COP-1 / FOP directive arguments
    public static final String ARGUMENT_FOP_DIRECTIVE_TC_VC_ID = "TCVCID";
    public static final String ARGUMENT_FOP_DIRECTIVE_DIRECTIVE_ID = "DIRECTIVE";
    public static final String ARGUMENT_FOP_DIRECTIVE_QUALIFIER = "QUALIFIER";
    public static final String ARGUMENT_TC_SET_AD = "SET_AD";
}
