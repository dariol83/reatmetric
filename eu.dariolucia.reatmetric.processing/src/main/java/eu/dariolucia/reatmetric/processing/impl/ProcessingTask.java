package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.processing.impl.graph.GraphModel;
import eu.dariolucia.reatmetric.processing.impl.operations.AbstractModelOperation;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProcessingTask implements Callable<List<AbstractDataItem>> {

    private static final Logger LOG = Logger.getLogger(ProcessingTask.class.getName());

    private List<AbstractModelOperation> operations;
    private final Consumer<List<AbstractDataItem>> output;
    private final Set<Integer> affectedItems;
    private final WorkingSet workingSet;

    ProcessingTask(List<AbstractModelOperation> operations, Consumer<List<AbstractDataItem>> output, WorkingSet workingSet) {
        this.operations = operations;
        this.output = output;
        this.workingSet = workingSet;
        this.affectedItems = new HashSet<>();
    }

    void prepareTask(GraphModel graphModel) {
        // Finalize the list by extending it with the necessary re-evaluations, the setting of the processors
        // and order by topological sort
        operations = graphModel.finalizeOperationList(operations);
        // Build the set of affected items by ID
        for (AbstractModelOperation amo : operations) {
            this.affectedItems.add(amo.getSystemEntityId());
        }
    }

    @Override
    public List<AbstractDataItem> call() {
        // XXX: think about having parallel parameter processing by introducing processing levels based on longest-dependency count
        List<AbstractDataItem> result = new ArrayList<>(operations.size());
        for (AbstractModelOperation amo : operations) {
            try {
                result.addAll(amo.execute());
            } catch (Exception e) {
                // You need to survive here!
                LOG.log(Level.SEVERE, "Cannot process model operation " + amo + ": " + e.getMessage(), e);
            }
        }
        workingSet.remove(affectedItems);
        // Notify
        output.accept(result);
        // Return the result
        return result;
    }

    Set<Integer> getAffectedItems() {
        return affectedItems;
    }
}
