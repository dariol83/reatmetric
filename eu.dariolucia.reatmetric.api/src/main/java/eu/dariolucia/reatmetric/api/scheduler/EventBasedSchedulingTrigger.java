package eu.dariolucia.reatmetric.api.scheduler;

/**
 * This class allows the scheduling of an activity invocation to after the completion of all
 * the referenced scheduled activities.
 */
public class EventBasedSchedulingTrigger extends AbstractSchedulingTrigger {

    private final int event;

    /**
     * The protection time in ms: a new activity occurrence is released only if a new occurrence of the
     * specified event happens at least protectionTime millisecond after the previous occurrence.
     * This mechanism avoids flooding the system with activity invocations in case of many occurrences
     * of the same event happening very close in time.
     */
    private final int protectionTime;

    public EventBasedSchedulingTrigger(int event, int protectionTime) {
        this.event = event;
        this.protectionTime = protectionTime;
    }

    public int getEvent() {
        return event;
    }

    public int getProtectionTime() {
        return protectionTime;
    }

    @Override
    public String toString() {
        return "EventBasedSchedulingTrigger{" +
                "event=" + event +
                ", protectionTime=" + protectionTime +
                "} " + super.toString();
    }
}
