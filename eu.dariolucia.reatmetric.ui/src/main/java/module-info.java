import eu.dariolucia.reatmetric.api.IServiceFactory;

open module eu.dariolucia.reatmetric.ui {

    requires java.logging;

    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.controls;

    requires eu.dariolucia.reatmetric.api;

    uses IServiceFactory;
    // Test system
    provides IServiceFactory with eu.dariolucia.reatmetric.ui.test.TestSystem;
}