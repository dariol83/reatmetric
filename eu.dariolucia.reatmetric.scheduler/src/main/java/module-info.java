open module eu.dariolucia.reatmetric.scheduler {

    requires java.logging;
    requires java.rmi;
    requires eu.dariolucia.reatmetric.api;

    exports eu.dariolucia.reatmetric.scheduler;

    provides eu.dariolucia.reatmetric.api.scheduler.ISchedulerFactory with eu.dariolucia.reatmetric.scheduler.SchedulerFactory;
}
