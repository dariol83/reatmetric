package eu.dariolucia.reatmetric.processing.impl;

import java.util.HashSet;
import java.util.Set;

public class WorkingSet {

    private final Set<Integer> workingSet = new HashSet<>();

    public boolean overlaps(Set<Integer> ids) {
        synchronized (workingSet) {
            return workingSet.stream().anyMatch(ids::contains);
        }
    }

    public void add(Set<Integer> ids) {
        synchronized (workingSet) {
            if(overlaps(ids)) {
                try {
                    workingSet.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            workingSet.addAll(ids);
        }
    }

    public void remove(Set<Integer> ids) {
        synchronized (workingSet) {
            workingSet.removeAll(ids);
            workingSet.notifyAll();
        }
    }
}
