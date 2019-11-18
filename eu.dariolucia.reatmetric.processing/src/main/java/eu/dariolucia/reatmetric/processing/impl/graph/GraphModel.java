package eu.dariolucia.reatmetric.processing.impl.graph;

import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.processing.definition.*;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.impl.processors.AbstractSystemEntityProcessor;
import eu.dariolucia.reatmetric.processing.impl.processors.ParameterProcessor;

import java.util.HashMap;
import java.util.Map;
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

    public void build() {
        // Navigate the model and add all the system entity nodes:
        // - parameters
        // - events
        // - containers
        for(ParameterProcessingDefinition param : definition.getParameterDefinitions()) {
            addEntities(param, () -> new ParameterProcessor(param, processingModel));
        }
        for(EventProcessingDefinition event : definition.getEventDefinitions()) {
            addEntities(event, () -> new EventProcessor(event, processingModel));
        }
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
            addParentDependencies(param);
        }
        for(EventProcessingDefinition event : definition.getEventDefinitions()) {
            if(event.getExpression() != null) {
                addEdges(event, event.getExpression());
            }
            addParentDependencies(event);
        }
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
            EntityVertex c = new EntityVertex(new ContainerProcessor(null, processingModel));
            pathMap.putIfAbsent(location, c);
            location = location.getParent();
        }
    }
}
