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
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceDataFilter;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.InstantCellFactory;
import eu.dariolucia.reatmetric.ui.utils.ParameterDisplayCoordinator;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class ActivityDataViewController extends AbstractDisplayController {

    // Live/retrieval controls
    @FXML
    protected ToggleButton liveTgl;
    @FXML
    protected Button goToStartBtn;
    @FXML
    protected Button goBackFastBtn;
    @FXML
    protected Button goBackOneBtn;
    @FXML
    protected Button goToEndBtn;
    @FXML
    protected Button goForwardFastBtn;
    @FXML
    protected Button goForwardOneBtn;
    @FXML
    protected Button selectTimeBtn;

    @FXML
    protected Button filterBtn;

    // Print button
    @FXML
    protected Button printBtn;

    // Progress indicator for data retrieval
    @FXML
    protected ProgressIndicator progressIndicator;

    // Table
    @FXML
    protected TreeTableView<ActivityOccurrenceDataWrapper> dataItemTableView;

    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, String> nameCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, String> stateCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, String> statusCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, String> sourceCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, String> typeCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, String> routeCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, Object> resultCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, Instant> genTimeCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, Instant> execTimeCol;
    @FXML
    private TreeTableColumn<ActivityOccurrenceDataWrapper, String> parentCol;

    protected volatile ActivityOccurrenceDataFilter currentActivityFilter = new ActivityOccurrenceDataFilter(null, null, null, null, null, null);

    // Popup selector for date/time
    protected final Popup dateTimePopup = new Popup();

    // Time selector controller
    protected DateTimePickerWidgetController dateTimePickerController;

    private volatile boolean live = false;

    @Override
    protected Window retrieveWindow() {
        return liveTgl.getScene().getWindow();
    }

    @Override
    protected void doInitialize(URL url, ResourceBundle rb) {
        this.dataItemTableView.setPlaceholder(new Label(""));

        this.goToStartBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goBackOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goBackFastBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goToEndBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goForwardOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.goForwardFastBtn.disableProperty().bind(this.liveTgl.selectedProperty());
        this.selectTimeBtn.disableProperty().bind(this.liveTgl.selectedProperty());

        this.dateTimePopup.setAutoHide(true);
        this.dateTimePopup.setHideOnEscape(true);

        try {
            URL datePickerUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/DateTimePickerWidget.fxml");
            FXMLLoader loader = new FXMLLoader(datePickerUrl);
            Parent dateTimePicker = loader.load();
            this.dateTimePickerController = loader.getController();
            this.dateTimePopup.getContent().addAll(dateTimePicker);
            // Load the controller hide with select
            this.dateTimePickerController.setActionAfterSelection(() -> {
                this.dateTimePopup.hide();
                moveToTime(this.dateTimePickerController.getSelectedTime());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO: set cell value factory

        this.genTimeCol.setCellFactory(InstantCellFactory.instantTreeCellFactory());
        this.execTimeCol.setCellFactory(InstantCellFactory.instantTreeCellFactory());
    }

    @FXML
    protected void selectTimeButtonSelected(ActionEvent e) {
        if (this.dateTimePopup.isShowing()) {
            this.dateTimePopup.hide();
        } else {
            Bounds b = this.selectTimeBtn.localToScreen(this.selectTimeBtn.getBoundsInLocal());
            this.dateTimePopup.setX(b.getMinX());
            this.dateTimePopup.setY(b.getMaxY());
            this.dateTimePopup.getScene().getRoot().getStylesheets().add(getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/css/MainView.css").toExternalForm());
            this.dateTimePopup.show(this.liveTgl.getScene().getWindow());
        }
    }

    @FXML
    protected void liveToggleSelected(ActionEvent e) {
        if (this.liveTgl.isSelected()) {
            startSubscription();
        } else {
            stopSubscription();
            updateSelectTime();
        }
    }

    @FXML
    protected void goToStart(ActionEvent e) {
        if (!isProgressBusy()) {
            moveToTime(Instant.EPOCH);
        }
    }

    @FXML
    protected void goBackOne(ActionEvent e) {
        if (!isProgressBusy()) {
            // TODO
        }
    }

    @FXML
    protected void goBackFast(ActionEvent e) {
        if (!isProgressBusy()) {
            // TODO
        }
    }

    @FXML
    protected void goToEnd(ActionEvent e) {
        if (!isProgressBusy()) {
            moveToTime(Instant.ofEpochSecond(3600 * 24 * 365 * 1000L));
        }
    }

    @FXML
    protected void goForwardOne(ActionEvent e) {
        if (!isProgressBusy()) {
            // TODO
        }
    }

    @FXML
    protected void goForwardFast(ActionEvent e) {
        if (!isProgressBusy()) {
            // TODO
        }
    }

    @FXML
    protected void filterButtonSelected(ActionEvent e) {
        if (!isProgressBusy()) {
            // TODO
        }
    }

    protected void moveToTime(Instant selectedTime) {
        this.selectTimeBtn.setText(formatTime(selectedTime));
        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                // TODO
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    private void restoreColumnConfiguration() {
        if (this.system != null) {
            // TODO TableViewUtil.restoreColumnConfiguration(this.system.getName(), this.user, doGetComponentId(), this.dataItemTableView);
        }
    }

    private void persistColumnConfiguration() {
        if (this.system != null) {
            // TODO TableViewUtil.persistColumnConfiguration(this.system.getName(), this.user, doGetComponentId(), this.dataItemTableView);
        }
    }

    public void startSubscription() {
        this.live = true;
    }

    public void stopSubscription() {
        this.live = false;
    }

    protected void updateFilter() {
        // TODO this.currentParameterFilter = new ParameterDataFilter(null, new ArrayList<>(this.path2wrapper.keySet()), null, null, null, null);
        // Update the subscriptions
        ParameterDisplayCoordinator.instance().filterUpdated();
    }

    protected void updateSelectTime() {
        // Take the latest generation time from the table
        if (this.dataItemTableView.getRoot().getChildren().isEmpty()) {
            this.selectTimeBtn.setText("---");
        } else {
            // TODO
        }
    }

    private void markProgressBusy() {
        this.progressIndicator.setVisible(true);
    }

    private void markProgressReady() {
        Platform.runLater(() -> {
            this.progressIndicator.setVisible(false);
        });
    }

    private boolean isProgressBusy() {
        return this.progressIndicator.isVisible();
    }

    @Override
    protected Control doBuildNodeForPrinting() {
        return null; // TODO TableViewUtil.buildNodeForPrinting(this.dataItemTableView);
    }

    @Override
    protected void doSystemDisconnected(IReatmetricSystem system, boolean oldStatus) {
        this.liveTgl.setSelected(false);
        if (oldStatus) {
            persistColumnConfiguration();
        }
    }

    public ActivityOccurrenceDataFilter getCurrentParameterFilter() {
        return currentActivityFilter;
    }

    @Override
    protected void doSystemConnected(IReatmetricSystem system, boolean oldStatus) {
        this.liveTgl.setSelected(true);
        // Restore column configuration
        restoreColumnConfiguration();
        // Start subscription if there
        if (this.liveTgl.isSelected()) {
            startSubscription();
        }
    }

    protected String doGetComponentId() {
        return "ParameterDataView";
    }

    public void updateDataItems(List<ParameterData> messages) {
        Platform.runLater(() -> {
            // TODO
            // this.dataItemTableView.refresh();
            updateSelectTime();
        });
    }

    @FXML
    protected void onDragOver(DragEvent event) {
        event.consume();
    }

    @FXML
    private void onDragEntered(DragEvent event) {
        event.consume();
    }

    @FXML
    private void onDragExited(DragEvent event) {
        event.consume();
    }

    @FXML
    private void onDragDropped(DragEvent event) {
        event.consume();
    }

    public boolean isLive() {
        return this.live;
    }

    @FXML
    public void onPurgeMenuItem(ActionEvent actionEvent) {

    }

    public static class ActivityOccurrenceDataWrapper {

        private final SystemEntityPath path;
        private final SimpleObjectProperty<ActivityOccurrenceData> property = new SimpleObjectProperty<>();

        private final SimpleObjectProperty<Instant> generationTime = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Instant> receptionTime = new SimpleObjectProperty<>();
        private final SimpleStringProperty rawValue = new SimpleStringProperty();
        private final SimpleStringProperty engValue = new SimpleStringProperty();
        private final SimpleObjectProperty<Validity> validity = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<AlarmState> alarmState = new SimpleObjectProperty<>();

        public ActivityOccurrenceDataWrapper(ActivityOccurrenceData data, SystemEntityPath path) {
            property.set(data);
            generationTime.set(data.getGenerationTime());
            // TODO
            this.path = path;
        }

        public void set(ActivityOccurrenceData data) {
            property.set(data);
            generationTime.set(data.getGenerationTime());
            // TODO
        }

        public ActivityOccurrenceData get() {
            return property.getValue();
        }

        public SimpleObjectProperty<Instant> generationTimeProperty() {
            return generationTime;
        }

        public SimpleObjectProperty<Instant> receptionTimeProperty() {
            return receptionTime;
        }

        public SimpleStringProperty rawValueProperty() {
            return rawValue;
        }

        public SimpleStringProperty engValueProperty() {
            return engValue;
        }

        public SimpleObjectProperty<Validity> validityProperty() {
            return validity;
        }

        public SimpleObjectProperty<AlarmState> alarmStateProperty() {
            return alarmState;
        }

        public SystemEntityPath getPath() {
            return this.path;
        }

    }

}
