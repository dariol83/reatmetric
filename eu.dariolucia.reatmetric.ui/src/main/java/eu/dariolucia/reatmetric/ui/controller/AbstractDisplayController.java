/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.controller;

import eu.dariolucia.reatmetric.api.IServiceFactory;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.plugin.IReatmetricServiceListener;
import eu.dariolucia.reatmetric.ui.utils.InstantCellFactory;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.print.*;
import javafx.scene.control.Control;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Scale;

import java.net.URL;
import java.time.Instant;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author dario
 */
public abstract class AbstractDisplayController implements Initializable, IReatmetricServiceListener {

    // Service availability control
    @FXML
    protected Circle serviceHealthStatus;

    // Info
    protected IServiceFactory system = null;
    protected String user = System.getProperty("user.name");

    protected boolean serviceConnected = false;

    /**
     * Initializes the controller class.
     */
    @Override
    public final void initialize(URL url, ResourceBundle rb) {
        doInitialize(url, rb);
        ReatmetricUI.selectedSystem().addSubscriber(this);
    }

    @FXML
    protected void printButtonSelected(ActionEvent e) {
        final Control n = doBuildNodeForPrinting();
        if(n != null) {
            ReatmetricUI.threadPool(getClass()).execute(() -> {
                Printer printer = Printer.getDefaultPrinter();
                PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);
                PrinterJob job = PrinterJob.createPrinterJob();
                
                double scaleX = pageLayout.getPrintableWidth() / n.getPrefWidth();
                Scale scale = new Scale(scaleX, scaleX); // Homogeneus scale, assuming width larger than height ... 
                n.getTransforms().add(scale);
                
                if (job != null && job.showPrintDialog(this.serviceHealthStatus.getScene().getWindow())) {
                    boolean success = job.printPage(pageLayout, n);
                    if (success) {
                        ReatmetricUI.setStatusLabel("Job printed successfully");
                        job.endJob();
                    } else {
                        ReatmetricUI.setStatusLabel("Error while printing job on printer " + job.getPrinter().getName());
                    }
                }
            });
        } else {
            ReatmetricUI.setStatusLabel("Printing not supported on this display");
        }
    }

    @Override
    public void startGlobalOperationProgress() {
        // Nothing here, subclasses can override
    }

    @Override
    public void stopGlobalOperationProgress() {
        // Nothing here, subclasses can override
    }

    @Override
    public void systemConnected(IServiceFactory system) {
        Platform.runLater(() -> {
            this.system = system;
            // Set indicator
            this.serviceHealthStatus.setFill(Paint.valueOf("#00ca00"));
            //
            boolean oldStatus = this.serviceConnected;
            this.serviceConnected = true;
            //
            doSystemConnected(system, oldStatus);
        });
    }

    @Override
    public void systemDisconnected(IServiceFactory system) {
        Platform.runLater(() -> {
            boolean oldStatus = this.serviceConnected;
            //
            doSystemDisconnected(system, oldStatus);
            this.system = null;
            // Set indicator
            this.serviceHealthStatus.setFill(Paint.valueOf("#003915"));
            //
            // this.serviceConnected = false;
        });
    }

    protected static String formatTime(Instant time) {
        if(time == null) {
            return "---";
        } else {
            return InstantCellFactory.DATE_TIME_FORMATTER.format(time);
        }
    }

    protected abstract void doInitialize(URL url, ResourceBundle rb);

    protected abstract Control doBuildNodeForPrinting();

    protected abstract void doSystemDisconnected(IServiceFactory system, boolean oldStatus);

    protected abstract void doSystemConnected(IServiceFactory system, boolean oldStatus);

}

