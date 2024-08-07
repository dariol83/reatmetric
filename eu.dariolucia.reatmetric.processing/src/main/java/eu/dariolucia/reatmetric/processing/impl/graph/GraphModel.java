/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.processing.impl.graph;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelVisitor;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.processing.scripting.IEntityBinding;
import eu.dariolucia.reatmetric.processing.definition.*;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.impl.operations.AbstractModelOperation;
import eu.dariolucia.reatmetric.processing.impl.processors.*;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GraphModel {

    private static final Logger LOG = Logger.getLogger(GraphModel.class.getName());
    private static final String CACHE_FILE_NAME = ".ordering.cache";

    private final ProcessingDefinition definition;

    private final ProcessingModelImpl processingModel;

    private final Map<SystemEntityPath, EntityVertex> pathMap = new HashMap<>();
    private final Map<Integer, EntityVertex> idMap = new HashMap<>();

    private EntityVertex root;

    public GraphModel(ProcessingDefinition processingDefinition, ProcessingModelImpl processingModel) {
        this.definition = processingDefinition;
        this.processingModel = processingModel;
    }

    public void build() throws ProcessingModelException {
        // Navigate the model and add all the system entity nodes:
        // - parameters
        // - events
        // - containers
        // - activities
        for(ParameterProcessingDefinition param : definition.getParameterDefinitions()) {
            addEntities(param, () -> new ParameterProcessor(param, processingModel));
        }
        for(EventProcessingDefinition event : definition.getEventDefinitions()) {
             addEntities(event, () -> new EventProcessor(event, processingModel));
        }
        for(ActivityProcessingDefinition act : definition.getActivityDefinitions()) {
            addEntities(act, () -> new ActivityProcessor(act, processingModel));
        }

        // Now add the links for:
        // - expressions (source value computation, validity, expression calibration, expression checks, activity verification expressions)
        // - parent/child relationship (error propagation)
        // - event expressions
        // - parameter triggers
        // - references of default values for activity arguments
        for(ParameterProcessingDefinition param : definition.getParameterDefinitions()) {
            // Mirrored parameters do not depend on anything
            if(param.isMirrored()) {
                continue;
            }
            if(param.getExpression() != null) {
                addEdges(param, param.getExpression());
            }
            if(param.getValidity() != null) {
                if (param.getValidity().getCondition() != null) {
                    addEdges(param, param.getValidity().getCondition());
                }
                if (param.getValidity().getMatch() != null) {
                    new DependencyEdge(getVertexOf(param.getId()), getVertexOf(param.getValidity().getMatch().getParameter().getId()));
                    if(param.getValidity().getMatch().getReference() != null) {
                        new DependencyEdge(getVertexOf(param.getId()), getVertexOf(param.getValidity().getMatch().getReference().getId()));
                    }
                }
            }
            if(param.getCalibrations() != null) {
                for(CalibrationDefinition cd : param.getCalibrations()) {
                    if(cd.getApplicability() != null) {
                        if (cd.getApplicability().getCondition() != null) {
                            addEdges(param, cd.getApplicability().getCondition());
                        }
                        if (cd.getApplicability().getMatch() != null) {
                            new DependencyEdge(getVertexOf(param.getId()), getVertexOf(cd.getApplicability().getMatch().getParameter().getId()));
                            if(cd.getApplicability().getMatch().getReference() != null) {
                                new DependencyEdge(getVertexOf(param.getId()), getVertexOf(cd.getApplicability().getMatch().getReference().getId()));
                            }
                        }
                    }
                    if(cd instanceof ExpressionCalibration) {
                        addEdges(param, ((ExpressionCalibration) cd).getDefinition());
                    }
                }
            }
            for(CheckDefinition cd : param.getChecks()) {
                if (cd.getApplicability() != null && cd.getApplicability().getCondition() != null) {
                    addEdges(param, cd.getApplicability().getCondition());
                }
                if(cd instanceof ExpressionCheck) {
                    addEdges(param, ((ExpressionCheck) cd).getDefinition());
                }
            }
            for(ParameterTriggerDefinition ptd : param.getTriggers()) {
                new DependencyEdge(getVertexOf(ptd.getEvent().getId()), getVertexOf(param.getId()));
            }
        }
        for(EventProcessingDefinition event : definition.getEventDefinitions()) {
            // Mirrored events do not depend on anything
            if(event.isMirrored()) {
                continue;
            }
            if(event.getCondition() != null) {
                addEdges(event, event.getCondition());
            }
        }
        for(ActivityProcessingDefinition act : definition.getActivityDefinitions()) {
            // Mirrored activities do not depend on anything
            if(act.isMirrored()) {
                continue;
            }
            if(act.getVerification() != null) {
                addEdges(act, act.getVerification());
            }
            // This is needed to block the injection of referenced parameters while a new activity occurrence is about to be released
            for(AbstractArgumentDefinition aad : act.getArguments()) {
                if(aad instanceof PlainArgumentDefinition) {
                    addArgumentDependency((PlainArgumentDefinition) aad, act);
                } else if(aad instanceof ArrayArgumentDefinition) {
                    addArgumentGroupDependency((ArrayArgumentDefinition) aad, act);
                }
            }
        }
        // Topological sort now and assignment of the orderingIds
        computeTopologicalOrdering();
    }

    private void addArgumentGroupDependency(ArrayArgumentDefinition agd, ActivityProcessingDefinition act) {
        for(AbstractArgumentDefinition aad : agd.getElements()) {
            if(aad instanceof PlainArgumentDefinition) {
                addArgumentDependency((PlainArgumentDefinition) aad, act);
            } else if(aad instanceof ArrayArgumentDefinition) {
                addArgumentGroupDependency((ArrayArgumentDefinition) aad, act);
            }
        }
    }

    private void addArgumentDependency(PlainArgumentDefinition ad, ActivityProcessingDefinition act) {
        if (ad.getDefaultValue() != null && ad.getDefaultValue() instanceof ReferenceDefaultValue) {
            new DependencyEdge(getVertexOf(act.getId()), getVertexOf(((ReferenceDefaultValue) ad.getDefaultValue()).getParameter().getId()));
        }
    }

    private void computeTopologicalOrdering() throws ProcessingModelException {
        LOG.info("Computing model process ordering");
        // If the cache folder is set, check if there is the cache file
        if(definition.getCacheFolder() != null) {
            File cache = new File(definition.getCacheFolder() + File.separator + CACHE_FILE_NAME);
            try {
                if (cache.exists()) {
                    // Try to load and apply the cache
                    applyCache(cache);
                    // Done
                    return;
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Cannot read/apply cache file at " + cache.getAbsolutePath() + ": " + e.getMessage());
            }
        }

        // If you reach this point, it means that you have to compute the topological sort yourself
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

        // If the cache folder is set, then generate a cache file
        if(definition.getCacheFolder() != null) {
            File cache = new File(definition.getCacheFolder() + File.separator + CACHE_FILE_NAME);
            try {
                if (cache.exists()) {
                    cache.delete();
                }
                cache.createNewFile();
                // Try to store the cache
                storeCache(cache);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Cannot create cache file at " + cache.getAbsolutePath() + ": " + e.getMessage());

            }
        }

        LOG.info("Model process ordering completed");
    }

    /**
     * The cache is a sequence of integer, in pairs: the first is the system entity ID, the second is the ordering number.
     *
     * @param cache the file containing the cached ordering
     * @throws IOException if there are issues when reading the file
     */
    private void storeCache(File cache) throws IOException {
        LOG.info("Storing orderings to cache file " + cache.getAbsolutePath());
        DataOutputStream dis = new DataOutputStream(new FileOutputStream(cache));
        for(Map.Entry<Integer, EntityVertex> entry : idMap.entrySet()) {
            dis.writeInt(entry.getKey());
            dis.writeInt(entry.getValue().getOrderingId());
        }
        dis.close();
        LOG.info("Cache file construction completed");
    }

    private void applyCache(File cache) throws IOException {
        LOG.info("Loading orderings from cache file " + cache.getAbsolutePath());
        int couplesToApply = idMap.size();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(cache))) {
            while (true) {
                int evId = dis.readInt();
                int ordening = dis.readInt();
                EntityVertex ev = idMap.get(evId);
                if (ev != null) {
                    ev.setOrderingId(ordening);
                    --couplesToApply;
                } else {
                    throw new IOException("System Entity ID " + evId + " not found in the definition map, cache file might be outdated or corrupted");
                }
            }
        } catch (EOFException e) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("End of cache file detected");
            }
            if(couplesToApply != 0) {
                // Bad
                throw new IOException("Cache file did not cover the complete set of definitions, " + couplesToApply + " without ordering number, cache file might be outdated or corrupted");
            }
        } finally {
            LOG.info("Cache file processing completed");
        }
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

    private void addEdges(AbstractProcessingDefinition owner, ExpressionDefinition expression) throws ProcessingModelException {
        // owner is affected, if any of the bindings in the expression definition is updated;
        // relationship is 'depends on' -> owner must be evaluated if one successor changes;
        // in other words: if an entity changes, all predecessors (direct and indirect) must be evaluated.
        EntityVertex source = getVertexOf(owner.getId());
        for(SymbolDefinition sd : expression.getSymbols()) {
            EntityVertex destination = getVertexOf(sd.getReference());
            if(source == null || destination == null) {
                // Definition error
                throw new ProcessingModelException("In definition " + owner.getLocation() + ", referenced item " + sd.getReference() + " not found");
            }
            // Auto-add to source and destination
            new DependencyEdge(source, destination);
        }
    }

    private EntityVertex getVertexOf(int id) {
        return idMap.get(id);
    }

    private void addEntities(AbstractProcessingDefinition param, Supplier<AbstractSystemEntityProcessor> processorBuilder) throws ProcessingModelException {
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
            // Check: if we are the root, then keep it
            if(location.getPathLength() == 1) {
                EntityVertex potentialRoot = pathMap.get(location);
                if(this.root != null && this.root != potentialRoot) {
                    // Problem
                    throw new ProcessingModelException("Double root defined: " + location.asString() + " and " + this.root.getProcessor().getEntityState().getPath().asString());
                } else {
                    this.root = potentialRoot;
                }
            }
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

    /**
     * This method expands the provided list of operations adding the required object re-evaluations, depending on the
     * dependencies of the affected processors. Objects that are weakly consistent remain in this list but do not trigger
     * expansions, i.e. dependant objects are not added.
     *
     * @param operations the list of operations to be performed
     * @return the extended list of operations to be performed, including dependency re-evaluation
     */
    public List<AbstractModelOperation<?>> finalizeOperationList(List<AbstractModelOperation<?>> operations, boolean includeWeaklyConsistent) {
        Set<Integer> alreadyPresent = new HashSet<>();
        List<AbstractModelOperation<?>> extendedOperations = new LinkedList<>();
        // Add the entity IDs to the alreadyPresent set
        operations.forEach(o -> alreadyPresent.add(o.getSystemEntityId()));
        for(AbstractModelOperation<?> operation : operations) {
            EntityVertex entityVertex = getVertexOf(operation.getSystemEntityId());
            if(entityVertex == null) {
                LOG.log(Level.SEVERE, "Cannot locate entity with ID " + operation.getSystemEntityId() + ", processing skipped");
                continue;
            }
            // Set the correct processors to the provided operations
            entityVertex.assignProcessor(operation);
            // Add the affected processors for evaluation, if the processor of the operation is not weakly consistent
            List<AbstractModelOperation<?>> updateOperationsForProvidedOperation = entityVertex.getUpdateOperationsForAffectedEntities(includeWeaklyConsistent);
            for (AbstractModelOperation<?> updateOperation : updateOperationsForProvidedOperation) {
                if (!alreadyPresent.contains(updateOperation.getSystemEntityId())) {
                    alreadyPresent.add(updateOperation.getSystemEntityId());
                    extendedOperations.add(updateOperation);
                }
            }
        }
        // Now add to extendedOperations all the operations provided in the list
        extendedOperations.addAll(operations);
        // Re-order according to topological sort and generation time in case of equal ordering ID
        extendedOperations.sort((a,b) -> {
            if(a.getOrderingId() < b.getOrderingId()) {
                return -1;
            } else if(a.getOrderingId() > b.getOrderingId()) {
                return 1;
            } else {
                return a.getTime().compareTo(b.getTime());
            }
        });
        // Return
        return extendedOperations;
    }

    public SystemEntity getRoot() throws ProcessingModelException {
        if(this.root != null) {
            return this.root.getProcessor().getEntityState();
        } else {
            throw new ProcessingModelException("No root node present");
        }
    }

    public SystemEntity getSystemEntityOf(int id) throws ProcessingModelException {
        EntityVertex ev = this.idMap.get(id);
        if(ev != null) {
            return ev.getProcessor().getEntityState();
        } else {
            throw new ProcessingModelException("ID " + id + " unknown");
        }
    }

    public List<SystemEntity> getContainedEntities(int id) throws ProcessingModelException {
        EntityVertex ev = this.idMap.get(id);
        if(ev.getProcessor() instanceof ContainerProcessor) {
            return ((ContainerProcessor) ev.getProcessor()).getContainedEntities();
        } else {
            throw new ProcessingModelException("ID " + id + " does not map to a container");
        }
    }

    public SystemEntityPath getPathOf(int id) throws ProcessingModelException {
        EntityVertex ev = this.idMap.get(id);
        if(ev != null) {
            return ev.getProcessor().getEntityState().getPath();
        } else {
            throw new ProcessingModelException("ID " + id + " unknown");
        }
    }

    public int getIdOf(SystemEntityPath path) throws ProcessingModelException {
        EntityVertex ev = this.pathMap.get(path);
        if(ev != null) {
            return ev.getProcessor().getSystemEntityId();
        } else {
            throw new ProcessingModelException("Path " + path + " unknown");
        }
    }

    public IEntityBinding getBinding(int systemEntityId) {
        return (IEntityBinding) getProcessor(systemEntityId);
    }

    public AbstractSystemEntityProcessor getProcessor(int systemEntityId) {
        EntityVertex ev = idMap.get(systemEntityId);
        if(ev == null) {
            return null;
        } else {
            return ev.getProcessor();
        }
    }

    public void navigate(IProcessingModelVisitor visitor) {
        if(this.root != null) {
            // Start from root
            this.root.getProcessor().visit(visitor);
        }
    }

    public List<AbstractDataItem> getByPath(List<SystemEntityPath> paths) throws ProcessingModelException {
        List<AbstractDataItem> toReturn = new LinkedList<>();
        for(SystemEntityPath p : paths) {
            EntityVertex ev = this.pathMap.get(p);
            if(ev != null) {
                ev.getProcessor().putCurrentStates(toReturn);
            } else {
                throw new ProcessingModelException("Path " + p + " unknown");
            }
        }
        return toReturn;
    }

    public List<AbstractDataItem> getById(List<Integer> ids) throws ProcessingModelException {
        List<AbstractDataItem> toReturn = new LinkedList<>();
        for(Integer i : ids) {
            EntityVertex ev = this.idMap.get(i);
            if(ev != null) {
                ev.getProcessor().putCurrentStates(toReturn);
            } else {
                throw new ProcessingModelException("ID " + i + " unknown");
            }
        }
        return toReturn;
    }

    public AbstractSystemEntityDescriptor getDescriptorOf(int externalId) throws ProcessingModelException {
        EntityVertex ev = this.idMap.get(externalId);
        if(ev != null) {
            return ev.getProcessor().getDescriptor();
        } else {
            throw new ProcessingModelException("ID " + externalId + " unknown");
        }
    }

    public AbstractSystemEntityDescriptor getDescriptorOf(SystemEntityPath path) throws ProcessingModelException {
        EntityVertex ev = pathMap.get(path);
        if(ev != null) {
            return ev.getProcessor().getDescriptor();
        } else {
            throw new ProcessingModelException("Path " + path + " unknown");
        }
    }
}
