/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.activity;

public enum ActivityReportState {
    EXPECTED, // The algorithm expects this stage to have happened, no confirmation of the stage received yet
    PENDING, // The algorithm is waiting for the confirmation of the stage
    OK, // The algorithm received confirmation that the stage was OK
    TIMEOUT, // The timeout linked to the activity phase expired
    FAIL, // The algorithm received confirmation that the stage was failed, but the activity might still proceed
    FATAL, // The algorithm received confirmation that the stage was failed, the activity shall be considered completed
    ERROR, // The verification expression failed its evaluation, the activity is completed
    UNKNOWN // The algorithm has no clue of what happened to this stage
}
