package eu.dariolucia.reatmetric.processing.impl.graph;

import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.definition.*;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.impl.operations.AbstractModelOperation;
import eu.dariolucia.reatmetric.processing.impl.processors.AbstractSystemEntityProcessor;
import eu.dariolucia.reatmetric.processing.impl.processors.ContainerProcessor;
import eu.dariolucia.reatmetric.processing.impl.processors.ParameterProcessor;

import java.util.*;
import java.util.function.Supplier;

public class GraphModel {

    private final ProcessingDefinition definition;

    private final ProcessingModelImpl processingModel;

    private final Map<SystemEntityPath, EntityVertex> pathMap = new HashMap<>();
    private final Map<Integer, EntityVertex> idMap = new HashMap<>();

    public GraphModel(ProcessingDefinition processingDefinition, ProcessingModelImpl processingModel) {
        this.definition = processingDefinition;
        this.processingModel = processingModel;
    }

    public void build() throws ProcessingModelException {
        // Navigate the model and add all the system entity nodes:
        // - parameters
        // - events
        // - containers
        for(ParameterProcessingDefinition param : definition.getParameterDefinitions()) {
            addEntities(param, () -> new ParameterProcessor(param, processingModel));
        }
        // TODO
        // for(EventProcessingDefinition event : definition.getEventDefinitions()) {
        //     addEntities(event, () -> new EventProcessor(event, processingModel));
        // }

        // Now add the links for:
        // - expressions (source value computation, validity, expression calibration, expression checks)
        // - parent/child relationship (error propagation)
        // - event expressions
        for(ParameterProcessingDefinition param : definition.getParameterDefinitions()) {
            if(param.getExpression() != null) {
                addEdges(param, param.getExpression());
            }
            if(param.getValidity() != null) {
                addEdges(param, param.getValidity());
            }
            if(param.getCalibration() != null && param.getCalibration() instanceof ExpressionCalibration) {
                addEdges(param, ((ExpressionCalibration) param.getCalibration()).getDefinition());
            }
            for(CheckDefinition cd : param.getChecks()) {
                if(cd instanceof ExpressionCheck) {
                    addEdges(param, ((ExpressionCheck) cd).getDefinition());
                }
            }
            // TODO: check if this is really needed: if kept, there is zero parallelisation, since the root node will be always blocked
            addParentDependencies(param);
        }
        for(EventProcessingDefinition event : definition.getEventDefinitions()) {
            if(event.getExpression() != null) {
                addEdges(event, event.getExpression());
            }
            addParentDependencies(event);
        }

        // Topological sort now and assignment of the orderingIds
        computeTopologicalOrdering();
    }

    private void computeTopologicalOrdering() throws ProcessingModelException {
        List<EntityVertex> result = new LinkedList<>();
        Set<EntityVertex> alreadyProcessed = new HashSet<>();
        List<EntityVertex> toProcess = new LinkedList<>(this.idMap.values());
        while(!toProcess.isEmpty()) {
            EntityVertex next = toProcess.remove(0);
            if(alreadyProcessed.contains(next)) {
                continue;
            }
            navigate(next, new HashSet<>(), alreadyProcessed, result);
        }
        // Iterate and set the orderingId
        for(int i = 0; i < result.size(); ++i) {
            EntityVertex ev = result.get(i);
            ev.setOrderingId(i);
        }
        // Done
    }

    private void navigate(EntityVertex next, Set<EntityVertex> alreadyInPath, Set<EntityVertex> alreadyProcessed, List<EntityVertex> result) throws ProcessingModelException {
        // Cycle-check
        if(alreadyInPath.contains(next)) {
            throw new ProcessingModelException("Cycle detected containing definition " + next.getSystemEntityId());
        }
        // If already processed, nothing to do
        if(alreadyProcessed.contains(next)) {
            return;
        }
        // Add yourself to the alreadyInPath
        alreadyInPath.add(next);
        // Navigate all your children recursively
        for(DependencyEdge de : next.getSuccessors()) {
            navigate(de.getDestination(), alreadyInPath, alreadyProcessed, result);
        }
        // Then add yourself to the result list
        result.add(next);
        alreadyProcessed.add(next);
        // Remove yourself from the alreadyInPath
        alreadyInPath.remove(next);
    }

    private void addParentDependencies(AbstractProcessingDefinition originatingChild) {
        String location = originatingChild.getLocation();
        SystemEntityPath path = SystemEntityPath.fromString(location);
        EntityVertex child = getVertexOf(originatingChild.getId());
        while((path = path.getParent()) != null) {
            EntityVertex ev = getVertexOf(path);
            // ev depends on child (I know, this looks ugly)
            new DependencyEdge(ev, child);
            // For the next cycle, ev becomes the child
            child = ev;
        }
    }

    private EntityVertex getVertexOf(SystemEntityPath path) {
        return this.pathMap.get(path);
    }

    private void addEdges(AbstractProcessingDefinition owner, ExpressionDefinition expression) throws ProcessingModelException {
        // owner is affected, if any of the bindings in the expression definition is updated;
        // relationship is 'depends on' -> owner must be evaluated if one successor changes;
        // in other words: if an entity changes, all predecessors (direct and indirect) must be evaluated.
        EntityVertex source = getVertexOf(owner.getId());
        for(SymbolDefinition sd : expression.getSymbols()) {
            EntityVertex destination = getVertexOf(sd.getReference().getId());
            if(source == null || destination == null) {
                // Definition error
                throw new ProcessingModelException("In definition " + owner.getLocation() + ", referenced item " + sd.getReference().getLocation() + " not found");
            }
            // Auto-add to source and destination
            new DependencyEdge(source, destination);
        }
    }

    private EntityVertex getVertexOf(int id) {
        return idMap.get(id);
    }

    private void addEntities(AbstractProcessingDefinition param, Supplier<AbstractSystemEntityProcessor> processorBuilder) {
        SystemEntityPath location = SystemEntityPath.fromString(param.getLocation());
        // Add the parameter
        AbstractSystemEntityProcessor definitionProcessor = processorBuilder.get();
        EntityVertex v = new EntityVertex(definitionProcessor);
        pathMap.put(location, v);
        idMap.put(param.getId(), v);
        // Add the containers, recursively
        location = location.getParent();
        while(location != null) {
            ContainerProcessor processor;
            // If processor for the path was not created, create it
            if(!pathMap.containsKey(location)) {
                // Create a new processor
                int containerId = generateContainerId(location);
                processor = new ContainerProcessor(new ContainerProcessor.Definition(containerId, "", location.asString()), processingModel);
                EntityVertex c = new EntityVertex(processor);
                pathMap.put(location, c);
                idMap.put(containerId, c);
            } else {
                // Get the existing processor: if it is not a ContainerProcessor then there is a bug
                processor = (ContainerProcessor) pathMap.get(location).getProcessor();
            }
            // Add child processor to container, remember the previous!
            processor.addChildProcessor(definitionProcessor);
            definitionProcessor = processor;
            // Move one level up
            location = location.getParent();
        }
    }

    private int generateContainerId(SystemEntityPath location) {
        String locationString = location.asString();
        // The id for container is negative, and it is set equals to the hashcode of the path (negated if needed)
        int derivedId = -Math.abs(locationString.hashCode());
        while(idMap.containsKey(derivedId)) {
            // Add a space to the locationString at the end, and keep going
            locationString += " ";
            derivedId = -Math.abs(locationString.hashCode());
        }
        return derivedId;
    }

    public List<AbstractModelOperation> finalizeOperationList(List<AbstractModelOperation> operations) {
        Set<Integer> alreadyPresent = new HashSet<>();
        List<AbstractModelOperation> extendedOperations = new LinkedList<>();
        // Add the entity IDs to the alreadyPresent set
        operations.forEach(o -> alreadyPresent.add(o.getSystemEntityId()));
        for(AbstractModelOperation operation : operations) {
            EntityVertex entityVertex = getVertexOf(operation.getSystemEntityId());
            // Set the correct processors to the provided operations
            entityVertex.assignProcessor(operation);
            // Add the affected processors for evaluation
            List<AbstractModelOperation> updateOperationsForProvidedOperation = entityVertex.getUpdateOperationsForAffectedEntities();
            for(AbstractModelOperation updateOperation : updateOperationsForProvidedOperation) {
                if(!alreadyPresent.contains(updateOperation.getSystemEntityId())) {
                    alreadyPresent.add(updateOperation.getSystemEntityId());
                    extendedOperations.add(updateOperation);
                }
            }
        }
        // Now add to extendedOperations all the operations provided in the list
        extendedOperations.addAll(operations);
        // Re-order according to topological sort
        extendedOperations.sort(Comparator.comparingInt(AbstractModelOperation::getOrderingId));
        // Return
        return extendedOperations;
    }
}
