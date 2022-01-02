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
import eu.dariolucia.reatmetric.ui.utils.FxUtils;
import eu.dariolucia.reatmetric.ui.utils.InstantCellFactory;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.stage.Window;

import java.net.URL;
import java.time.Instant;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author Dario Lucia
 */
public abstract class AbstractDisplayController implements Initializable, IReatmetricServiceListener {

    private static final Logger LOG = Logger.getLogger(AbstractDisplayController.class.getName());

    protected static final double TABLE_ROW_HEIGHT = 24;

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
        final Node n = doBuildNodeForPrinting();
        if (n != null) {
            FxUtils.runLater(() -> { // Needed to allow the stage to show up and layout the element
                Printer printer = Printer.getDefaultPrinter();
                PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);
                PrinterJob job = PrinterJob.createPrinterJob();

                double scaleX = pageLayout.getPrintableWidth() / n.getBoundsInLocal().getWidth();
                Scale scale = new Scale(scaleX, scaleX); // Homogeneus scale, assuming width larger than height ...
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Printing scale is " + scale);
                }
                /*
                 * Scale-up idea from:
                 * https://stackoverflow.com/questions/32288411/how-to-save-a-high-dpi-snapshot-of-a-javafx-canvas/32297827#32297827
                 */
                SnapshotParameters parameters = new SnapshotParameters();
                parameters.setTransform(Transform.scale(4, 4));

                final WritableImage snapshot = new WritableImage((int) (n.getBoundsInLocal().getWidth() * 4), (int) (n.getBoundsInLocal().getHeight() * 4));
                n.snapshot(parameters, snapshot);
                ImageView view = new ImageView(snapshot);
                view.getTransforms().add(new Scale(scaleX / 4, scaleX / 4));
                if (job != null && job.showPrintDialog(retrieveWindow())) {
                    boolean success = job.printPage(pageLayout, view);
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

    public void dispose() {
        LOG.fine("Disposing controller " + getClass().getSimpleName() + " - " + this);
        systemDisconnected(null);
        ReatmetricUI.selectedSystem().removeSubscriber(this);
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
        FxUtils.runLater(() -> {
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
        FxUtils.runLater(() -> {
            boolean oldStatus = this.serviceConnected;
            //
            doSystemDisconnected(system, oldStatus);
            this.system = null;
            //
            // this.serviceConnected = false;
        });
    }

    protected static String formatTime(Instant time) {
        if (time == null) {
            return "---";
        } else {
            return InstantCellFactory.DATE_TIME_FORMATTER.format(time);
        }
    }

    protected static int getNumVisibleRow(TableView<?> table) {
        double h = table.getHeight();
        h -= 30; // Header
        double csize = table.getFixedCellSize();
        return (int) (h / csize);
    }

    protected static int getNumVisibleRow(TreeTableView<?> table) {
        double h = table.getHeight();
        h -= 30; // Header
        double csize = table.getFixedCellSize();
        return (int) (h / csize);
    }

    protected abstract void doInitialize(URL url, ResourceBundle rb);

    protected abstract Node doBuildNodeForPrinting();

    protected abstract void doSystemDisconnected(IReatmetricSystem system, boolean oldStatus);

    protected abstract void doSystemConnected(IReatmetricSystem system, boolean oldStatus);

    protected void informDisplayDetached() {
        // Subclasses can override
    }

    protected void informDisplayAttached() {
        // Subclasses can override
    }

    protected void initialiseToolbarVisibility(TitledPane pane, ToolBar toolbar, CheckMenuItem linkedMenuEntry) {
        toolbar.visibleProperty().bind(linkedMenuEntry.selectedProperty());
        toolbar.visibleProperty().addListener((observableValue, oldValue, newValue) -> {
            VBox vbox = (VBox) pane.getContent();
            if(newValue) {
                // Visible
                if(vbox.getChildren().get(0) != toolbar) {
                    vbox.getChildren().add(0, toolbar);
                }
            } else {
                // Not visible
                if(vbox.getChildren().get(0) == toolbar) {
                    vbox.getChildren().remove(0);
                }
            }
            pane.layout();
        });
    }
}

