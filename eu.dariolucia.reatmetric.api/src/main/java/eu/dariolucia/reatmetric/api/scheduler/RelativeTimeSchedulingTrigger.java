package eu.dariolucia.reatmetric.api.scheduler;

import eu.dariolucia.reatmetric.api.common.IUniqueId;

import java.util.List;

/**
 * This class allows the scheduling of an activity invocationto start after the completion of all
 * the referenced scheduled activities.
 */
public class RelativeTimeSchedulingTrigger extends AbstractSchedulingTrigger {

    private final List<IUniqueId> predecessors;

    public RelativeTimeSchedulingTrigger(List<IUniqueId> predecessors) {
        this.predecessors = List.copyOf(predecessors);
    }

    public List<IUniqueId> getPredecessors() {
        return predecessors;
    }

    @Override
    public String toString() {
        return "RelativeTimeSchedulingTrigger{" +
                "predecessors=" + predecessors +
                "} " + super.toString();
    }
}
