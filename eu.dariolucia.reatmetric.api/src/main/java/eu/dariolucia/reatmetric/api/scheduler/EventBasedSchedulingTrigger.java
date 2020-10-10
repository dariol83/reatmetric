package eu.dariolucia.reatmetric.api.scheduler;

import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

/**
 * This class allows the scheduling of an activity invocation to after the completion of all
 * the referenced scheduled activities.
 */
public class EventBasedSchedulingTrigger extends AbstractSchedulingTrigger {

    private final SystemEntityPath event;

    private final int protectionTime;

    private final boolean enabled;

    public EventBasedSchedulingTrigger(SystemEntityPath event, int protectionTime, boolean enabled) {
        this.event = event;
        this.protectionTime = protectionTime;
        this.enabled = enabled;
    }

    public SystemEntityPath getEvent() {
        return event;
    }

    /**
     * The protection time in ms: a new activity occurrence is released only if a new occurrence of the
     * specified event happens at least protectionTime millisecond after the previous occurrence.
     * This mechanism avoids flooding the system with activity invocations in case of many occurrences
     * of the same event happening very close in time.
     *
     * @return the protection time
     */
    public int getProtectionTime() {
        return protectionTime;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "[event " + event +
                ", protection-time " + protectionTime + " ms" +
                ", " + (enabled ? "enabled" : "disabled") +
                "]";
    }
}
