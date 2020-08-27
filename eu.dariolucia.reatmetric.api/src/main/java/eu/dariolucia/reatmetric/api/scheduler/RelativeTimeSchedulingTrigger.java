package eu.dariolucia.reatmetric.api.scheduler;

import java.util.List;

/**
 * This class allows the scheduling of an activity invocation to start after the completion of all
 * the referenced (by external id) scheduled activities.
 *
 * Optionally, a delay time in seconds since the completion of the last scheduled activity can be provided.
 */
public class RelativeTimeSchedulingTrigger extends AbstractSchedulingTrigger {

    private final List<Long> predecessors;

    private final int delayTime;

    public RelativeTimeSchedulingTrigger(List<Long> predecessors) {
        this(predecessors, 0);
    }

    public RelativeTimeSchedulingTrigger(List<Long> predecessors, int delayTime) {
        if(delayTime < 0) {
            throw new IllegalArgumentException("Delay time is less than 0");
        }
        this.predecessors = List.copyOf(predecessors);
        this.delayTime = delayTime;
    }

    public int getDelayTime() {
        return delayTime;
    }

    public List<Long> getPredecessors() {
        return predecessors;
    }

    @Override
    public String toString() {
        return "RelativeTimeSchedulingTrigger{" +
                "predecessors=" + predecessors +
                ", delayTime=" + delayTime +
                "} " + super.toString();
    }
}
