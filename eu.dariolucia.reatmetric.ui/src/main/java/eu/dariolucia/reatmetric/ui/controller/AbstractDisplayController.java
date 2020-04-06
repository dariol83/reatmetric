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


package eu.dariolucia.reatmetric.ui.controller;

import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
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
import javafx.stage.Window;

import java.net.URL;
import java.time.Instant;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author dario
 */
public abstract class AbstractDisplayController implements Initializable, IReatmetricServiceListener {

    // Info
    protected IReatmetricSystem system = null;
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
                
                if (job != null && job.showPrintDialog(retrieveWindow())) {
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

    protected abstract Window retrieveWindow();

    @Override
    public void startGlobalOperationProgress() {
        // Nothing here, subclasses can override
    }

    @Override
    public void stopGlobalOperationProgress() {
        // Nothing here, subclasses can override
    }

    @Override
    public void systemStatusUpdate(SystemStatus status) {
        // Nothing here, subclasses can override
    }

    @Override
    public void systemConnected(IReatmetricSystem system) {
        Platform.runLater(() -> {
            this.system = system;
            //
            boolean oldStatus = this.serviceConnected;
            this.serviceConnected = true;
            //
            doSystemConnected(system, oldStatus);
        });
    }

    @Override
    public void systemDisconnected(IReatmetricSystem system) {
        Platform.runLater(() -> {
            boolean oldStatus = this.serviceConnected;
            //
            doSystemDisconnected(system, oldStatus);
            this.system = null;
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

    protected abstract void doSystemDisconnected(IReatmetricSystem system, boolean oldStatus);

    protected abstract void doSystemConnected(IReatmetricSystem system, boolean oldStatus);

}

