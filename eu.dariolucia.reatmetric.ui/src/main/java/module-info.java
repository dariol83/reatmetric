import eu.dariolucia.reatmetric.api.IReatmetricSystem;

open module eu.dariolucia.reatmetric.ui {

    requires java.logging;

    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.controls;

    requires org.controlsfx.controls;

    requires eu.dariolucia.reatmetric.api;

    exports eu.dariolucia.reatmetric.ui;

    uses IReatmetricSystem;
}