/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.common;

public class Constants {
    // RawData Names
    public static final String N_TM_TRANSFER_FRAME = "TRANSFER FRAME";
    public static final String N_UNKNOWN_PACKET = "UNKNOWN";
    public static final String N_TIME_COEFFICIENTS = "TIME COEFFICIENT";

    // RawData Types
    public static final String T_TM_FRAME = "TM FRAME";
    public static final String T_AOS_FRAME = "AOS FRAME";
    public static final String T_TM_PACKET = "TM PACKET";
    public static final String T_IDLE_PACKET = "IDLE PACKET";
    public static final String T_TC_PACKET = "TC PACKET";
    public static final String T_BAD_TM = "BAD TM";
    public static final String T_BAD_PACKET = "BAD PACKET";
    public static final String T_UNKNOWN_PACKET = "UNKNOWN PACKET";
    public static final String T_TIME_COEFFICIENTS = "TIME COEFF";

    // Annotations at AbstractTransferFrame/SpacePacket level
    public static final String ANNOTATION_ROUTE = "##ROUTE";
    public static final String ANNOTATION_SOURCE = "##SOURCE";
    public static final String ANNOTATION_GEN_TIME = "##GENTIME";
    public static final String ANNOTATION_RCP_TIME = "##RCPTIME";
    public static final String ANNOTATION_TM_PUS_HEADER = "##TM_PUS_HEADER";
    public static final String ANNOTATION_UNIQUE_ID = "##UNIQUE_ID";

    // encdec definition: TM/TC packet type
    public static final String ENCDEC_TM_PACKET_TYPE = "TM";
    public static final String ENCDEC_TC_PACKET_TYPE = "TC";

    // Processing model: event types
    public static final String EVT_T_ONBOARD_EVENT = "ONBOARD EVENT";
    public static final String EVT_T_TC_VERIFICATION_EVENT = "TC VERIFICATION";
}
