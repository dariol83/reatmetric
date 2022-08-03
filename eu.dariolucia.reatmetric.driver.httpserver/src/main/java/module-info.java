open module eu.dariolucia.reatmetric.driver.httpserver {
    requires java.logging;
    requires java.xml.bind;

    requires jdk.httpserver;
    requires json.path;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.core;
    requires java.rmi;

    exports eu.dariolucia.reatmetric.driver.httpserver;

    provides eu.dariolucia.reatmetric.core.api.IDriver with eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;
}