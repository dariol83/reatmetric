/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.controller;

import java.net.URL;
import java.util.ResourceBundle;

import eu.dariolucia.reatmetric.api.IServiceFactory;
import eu.dariolucia.reatmetric.api.common.IServiceMonitorCallback;
import eu.dariolucia.reatmetric.api.common.IUserMonitorCallback;
import eu.dariolucia.reatmetric.api.common.ServiceType;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.plugin.IMonitoringCentreServiceListener;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterAttributes;
import javafx.print.PrinterJob;
import javafx.scene.control.Control;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Scale;

/**
 * FXML Controller class
 *
 * @author dario
 */
public abstract class AbstractDisplayController implements Initializable, IMonitoringCentreServiceListener, IUserMonitorCallback, IServiceMonitorCallback {
 
    // Service availability control
    @FXML
    protected Circle serviceHealthStatus;

    // Info
    protected String system = null;
    protected String user = null;
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
                PrinterAttributes attr = printer.getPrinterAttributes();
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
    public void systemAdded(IServiceFactory system) {
        system.register((IUserMonitorCallback) this);
        system.register((IServiceMonitorCallback) this);
    }

    @Override
    public void systemRemoved(IServiceFactory system) {
        system.deregister((IUserMonitorCallback) this);
        system.deregister((IServiceMonitorCallback) this);
    }

    @Override
    public void userDisconnected(String system, String user) {
        Platform.runLater(() -> {
            this.system = null;
            this.user = null;
            doUserDisconnected(system, user);
        });
    }

    @Override
    public void userConnected(String system, String user) {
        Platform.runLater(() -> {
            this.system = system;
            this.user = user;
            doUserConnected(system, user);
        });
    }

    @Override
    public void userConnectionFailed(String system, String user, String reason) {
        Platform.runLater(() -> {
            this.system = null;
            this.user = null;
            doUserConnectionFailed(system, user, reason);
        });
    }

    @Override
    public void serviceDisconnected(String system, ServiceType service) {
        if (service.equals(doGetSupportedService())) {
            Platform.runLater(() -> {
                // Set indicator
                this.serviceHealthStatus.setFill(Paint.valueOf("#003915"));
                //
                boolean oldStatus = this.serviceConnected;
                this.serviceConnected = false;
                //
                doServiceDisconnected(oldStatus);           
            });
        }
    }

    @Override
    public void serviceConnected(String system, ServiceType service) {
        if (service.equals(doGetSupportedService())) {
            Platform.runLater(() -> {
                // Set indicator
                this.serviceHealthStatus.setFill(Paint.valueOf("#00ca00"));
                // 
                boolean oldStatus = this.serviceConnected;
                this.serviceConnected = true;
                //
                doServiceConnected(oldStatus);
            });
        }
    }
    
    protected abstract void doInitialize(URL url, ResourceBundle rb);

    protected abstract Control doBuildNodeForPrinting();

    protected abstract void doUserDisconnected(String system, String user);

    protected abstract void doUserConnected(String system, String user);

    protected abstract void doUserConnectionFailed(String system, String user, String reason);

    protected abstract void doServiceDisconnected(boolean previousConnectionStatus);

    protected abstract void doServiceConnected(boolean previousConnectionStatus);
    
    protected abstract ServiceType doGetSupportedService();
}

