/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.stubs;

import eu.dariolucia.reatmetric.processing.ActivityHandlingException;
import eu.dariolucia.reatmetric.processing.IActivityHandler;
import eu.dariolucia.reatmetric.processing.IProcessingModel;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ActivityHandlerStub implements IActivityHandler {

    private static final Logger LOG = Logger.getLogger(ActivityHandlerStub.class.getName());

    public static ActivityHandlerStubBuilder create() {
        return new ActivityHandlerStubBuilder();
    }

    private final List<String> routes;
    private final List<String> types;
    private final Set<String> unavailableRoutes = new TreeSet<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4, (t) -> {
        Thread toReturn = new Thread(t, "Activity Handler Thread");
        toReturn.setDaemon(true);
        return toReturn;
    });
    private volatile LifecycleStrategy lifecycle = new NominalLifecycleStrategy();

    private IProcessingModel model;

    public ActivityHandlerStub(List<String> routes, List<String> types) {
        this.routes = routes;
        this.types = types;
    }

    @Override
    public void registerModel(IProcessingModel model) {
        this.model = model;
    }

    @Override
    public void deregisterModel(IProcessingModel model) {
        this.model = null;
    }

    @Override
    public List<String> getSupportedRoutes() {
        return this.routes;
    }

    @Override
    public List<String> getSupportedActivityTypes() {
        return this.types;
    }

    @Override
    public void executeActivity(ActivityInvocation activityInvocation) throws ActivityHandlingException {
        LOG.info("Activity invocation: " + activityInvocation);
        if(model == null) {
            throw new ActivityHandlingException("Activity handler not registered");
        }
        if(unavailableRoutes.contains(activityInvocation.getRoute())) {
            throw new ActivityHandlingException("Route " + activityInvocation.getRoute() + " currently not available");
        }
        executor.submit(() -> lifecycle.execute(activityInvocation, model));
    }

    public void setLifecycle(LifecycleStrategy lifecycle) {
        this.lifecycle = lifecycle;
    }

    public static class ActivityHandlerStubBuilder {
        private List<String> routes;
        private List<String> types;
        private LifecycleStrategy lifecycle = new NominalLifecycleStrategy();

        public ActivityHandlerStubBuilder withLifecycle(LifecycleStrategy strategy) {
            this.lifecycle = strategy;
            return this;
        }

        public ActivityHandlerStubBuilder withRoutes(String... routes) {
            this.routes = Arrays.asList(routes);
            return this;
        }

        public ActivityHandlerStubBuilder withTypes(String... types) {
            this.types = Arrays.asList(types);
            return this;
        }

        public ActivityHandlerStub build() {
            ActivityHandlerStub stub = new ActivityHandlerStub(routes, types);
            if(lifecycle != null) {
                stub.setLifecycle(lifecycle);
            }
            return stub;
        }
    }
}
