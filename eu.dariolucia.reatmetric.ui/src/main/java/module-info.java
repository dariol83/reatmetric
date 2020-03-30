import eu.dariolucia.reatmetric.api.IReatmetricSystem;

open module eu.dariolucia.reatmetric.ui {

    requires java.logging;
    requires java.desktop;

    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.web;

    requires org.controlsfx.controls;

    requires eu.dariolucia.reatmetric.api;

    exports eu.dariolucia.reatmetric.ui;

    uses IReatmetricSystem;
}