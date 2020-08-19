package eu.dariolucia.reatmetric.api.scheduler;

import java.time.Instant;

/**
 * This class allows the scheduling of an activity invocation using a predefined release time.
 */
public class AbsoluteTimeSchedulingTrigger extends AbstractSchedulingTrigger {

    private final Instant releaseTime;

    public AbsoluteTimeSchedulingTrigger(Instant releaseTime) {
        this.releaseTime = releaseTime;
    }

    public Instant getReleaseTime() {
        return releaseTime;
    }

    @Override
    public String toString() {
        return "AbsoluteTimeSchedulingTrigger{" +
                "releaseTime=" + releaseTime +
                "} " + super.toString();
    }
}
