package eu.dariolucia.reatmetric.scheduler.impl;

import java.time.Duration;
import java.time.Instant;

// TODO: add this in the ScheduledActivityDataItem (expectedStartTime, expectedEndTime) and for persistence, then remove this class
public class TimeInformation {

    private final Instant startTime;
    private final Instant endTime;

    public TimeInformation(Instant startTime, Instant endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public Duration getExpectedDuration() {
        return Duration.between(startTime, endTime);
    }

    public boolean overlapsWith(TimeInformation requestTimeInfo) {
        return (startTime.isAfter(requestTimeInfo.startTime) && startTime.isBefore(requestTimeInfo.endTime)) ||
                (endTime.isAfter(requestTimeInfo.startTime) && endTime.isBefore(requestTimeInfo.endTime));
    }
}
