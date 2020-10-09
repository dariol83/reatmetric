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
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.DataProcessingDelegator;
import eu.dariolucia.reatmetric.ui.utils.FxUtils;
import eu.dariolucia.reatmetric.ui.utils.TableViewUtil;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * FXML Controller class
 *
 * @author dario
 */
public abstract class AbstractDataItemLogViewController<T extends AbstractDataItem, V extends AbstractDataItemFilter<T>> extends AbstractDisplayController {

    protected static final int MAX_ENTRIES = 100;

    // Pane control
    @FXML
    protected TitledPane displayTitledPane;

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
    
    @FXML
    protected Button printBtn;

    // Progress indicator for data retrieval
    @FXML
    protected ProgressIndicator progressIndicator;

    // Table
    @FXML
    protected TableView<T> dataItemTableView;

    // Filtered items
    protected FilteredList<T> filteredItemList;

    protected ObservableList<T> dataItemList;

    // Popup selector for date/time
    protected final Popup dateTimePopup = new Popup();

    // Popup selector for filter
    protected final Popup filterPopup = new Popup();
    
    // Time selector controller
    protected DateTimePickerWidgetController dateTimePickerController;
    
    // Filter controller
    protected IFilterController<V> dataItemFilterController;
    
    // Temporary object queue
    protected DataProcessingDelegator<T> delegator;

    /**
     * Initializes the controller class.
     */
    @Override
    protected void doInitialize(URL url, ResourceBundle rb) {
    	this.dataItemTableView.setPlaceholder(new Label(""));
    	
    	if(this.liveTgl != null) {
	        this.goToStartBtn.disableProperty().bind(this.liveTgl.selectedProperty());
	        this.goBackFastBtn.disableProperty().bind(this.liveTgl.selectedProperty());
	        this.goBackOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
	        this.goToEndBtn.disableProperty().bind(this.liveTgl.selectedProperty());
	        this.goForwardFastBtn.disableProperty().bind(this.liveTgl.selectedProperty());
	        this.goForwardOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
	        this.selectTimeBtn.disableProperty().bind(this.liveTgl.selectedProperty());
    	}
    	
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
                moveToTime(this.dateTimePickerController.getSelectedTime(), RetrievalDirection.TO_PAST, getNumVisibleRow() * 2, this.dataItemFilterController.getSelectedFilter());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        this.filterPopup.setAutoHide(true);
        this.filterPopup.setHideOnEscape(true);
        
        try {
            URL filterWidgetUrl = doGetFilterWidget();
            FXMLLoader loader = new FXMLLoader(filterWidgetUrl);
            Parent filterSelector = loader.load();
            this.dataItemFilterController = loader.getController();
            this.filterPopup.getContent().addAll(filterSelector);
            // Load the controller hide with select
            this.dataItemFilterController.setActionAfterSelection(() -> {
                this.filterPopup.hide();
                applyFilter(this.dataItemFilterController.getSelectedFilter());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.delegator = new DataProcessingDelegator<>(doGetComponentId(), buildIncomingDataDelegatorAction());

        // Enable filtering
        dataItemList = dataItemTableView.getItems();
        filteredItemList = new FilteredList<>(dataItemList, p -> true);
        dataItemTableView.setItems(filteredItemList);
    }

    protected Consumer<List<T>> buildIncomingDataDelegatorAction() {
        return (List<T> t) -> addDataItems(t, true);
    }
    
    @FXML
    protected void liveToggleSelected(ActionEvent e) {
        if (this.liveTgl.isSelected()) {
        	clearTable();
        	moveToTime(Instant.now(), RetrievalDirection.TO_PAST, getNumVisibleRow(), this.dataItemFilterController.getSelectedFilter());
            startSubscription();
        } else {
            stopSubscription();
            updateSelectTime();
        }
    }

    @FXML
    protected void filterButtonSelected(ActionEvent e) {
        if (this.filterPopup.isShowing()) {
            this.filterPopup.hide();
        } else {
            Bounds b = this.filterBtn.localToScreen(this.filterBtn.getBoundsInLocal());
            this.filterPopup.setX(b.getMinX());
            this.filterPopup.setY(b.getMaxY());
            this.filterPopup.getScene().getRoot().getStylesheets().add(getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/css/MainView.css").toExternalForm());
            this.filterPopup.show(this.displayTitledPane.getScene().getWindow());
        }
    }

    @FXML
    protected void goToStart(ActionEvent e) {
        if(isProcessingAvailable()) {
            moveToTime(Instant.EPOCH, RetrievalDirection.TO_FUTURE, 1, this.dataItemFilterController.getSelectedFilter());
        }
    }

    @FXML
    protected void goBackFast(ActionEvent e) {
        if(isProcessingAvailable()) {
            fetchRecords(getNumVisibleRow(), RetrievalDirection.TO_PAST);
        }
    }

    @FXML
    protected void goBackOne(ActionEvent e) {
        if(isProcessingAvailable()) {
            fetchRecords(1, RetrievalDirection.TO_PAST);
        }
    }

    @FXML
    protected void goToEnd(ActionEvent e) {
        if(isProcessingAvailable()) {
            moveToTime(Instant.ofEpochSecond(3600*24*365*1000L), RetrievalDirection.TO_PAST, getNumVisibleRow() * 2, this.dataItemFilterController.getSelectedFilter());
        }
    }

    @FXML
    protected void goForwardFast(ActionEvent e) {
        if(isProcessingAvailable()) {
            fetchRecords(getNumVisibleRow(), RetrievalDirection.TO_FUTURE);
        }
    }

    @FXML
    protected void goForwardOne(ActionEvent e) {
        if(isProcessingAvailable()) {
            fetchRecords(1, RetrievalDirection.TO_FUTURE);
        }
    }

    protected void fetchRecords(int n, RetrievalDirection direction) {
        if(dataItemTableView.getItems().isEmpty()) {
            return;
        }
        // Get the first message in the table
        T om = direction == RetrievalDirection.TO_FUTURE ? getFirst() : getLast();
        // Retrieve the next one and add it on top
        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<T> messages = doRetrieve(om, n, direction, this.dataItemFilterController.getSelectedFilter());
                if (direction == RetrievalDirection.TO_FUTURE) {
                    // Reverse the list before adding it
                    Collections.reverse(messages);
                    addDataItems(messages, false);
                } else {
                    addDataItemsBack(messages, n, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
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
            this.dateTimePopup.show(this.displayTitledPane.getScene().getWindow());
        }
    }

    @Override
    protected void doSystemDisconnected(IReatmetricSystem system, boolean oldState) {
        if(this.liveTgl != null) {
            this.liveTgl.setSelected(false);
        }
        this.displayTitledPane.setDisable(true);

        if(oldState) {
            persistColumnConfiguration(system);
        }
        // Clear the table                
        clearTable();
    }

    @Override
    protected void doSystemConnected(IReatmetricSystem system, boolean oldState) {
        if(this.liveTgl != null) {
            this.liveTgl.setSelected(true);
        }
        this.displayTitledPane.setDisable(false);

        // Restore column configuration
        restoreColumnConfiguration();
        // Start subscription if there
        if (this.liveTgl == null || this.liveTgl.isSelected()) {
            // Allow table to be present and layed out before getting its size
            FxUtils.runLater(() -> {
                clearTable();
                moveToTime(Instant.now(), RetrievalDirection.TO_PAST, getNumVisibleRow(), this.dataItemFilterController.getSelectedFilter());
                startSubscription();
            });
        }
    }
    
    private void restoreColumnConfiguration() {
        if(system != null) {
            try {
                TableViewUtil.restoreColumnConfiguration(this.system.getName(), this.user, doGetComponentId(), this.dataItemTableView);
            } catch (RemoteException e) {
                // Nothing
                e.printStackTrace();
            }
        }
    }
    
    private void persistColumnConfiguration(IReatmetricSystem system) {
        if(system != null) {
            try {
                TableViewUtil.persistColumnConfiguration(system.getName(), this.user, doGetComponentId(), this.dataItemTableView);
            } catch (RemoteException e) {
                // Nothing
                e.printStackTrace();
            }
        }
    }

    protected void informDataItemsReceived(List<T> objects) {
        this.delegator.delegate(objects);
    }

    protected void addDataItems(List<T> messages, boolean fromLive) {
        if(fromLive) {
            // Revert the list
            Collections.reverse(messages);
        }
        FxUtils.runLater(() -> {
            if (!this.displayTitledPane.isDisabled() && (!fromLive || (this.liveTgl == null || this.liveTgl.isSelected()))) {
                this.dataItemList.addAll(0, messages);
                if (this.filteredItemList.size() > MAX_ENTRIES) {
                    int toRemove = dataItemList.size() - MAX_ENTRIES;
                    dataItemList.remove(dataItemList.size() - toRemove, dataItemList.size());
                }
                if (!fromLive) {
                    this.dataItemTableView.scrollTo(0);
                }
                updateSelectTime();
            }
        });
    }

    protected void addDataItemsBack(List<T> messages, int n, boolean clearTable) {
        FxUtils.runLater(() -> {
            if (!this.displayTitledPane.isDisabled()) {
                if (clearTable) {
                    clearTable();
                }
                int toRemoveTop = dataItemList.size() > n ? n : dataItemList.size() - 1;
                if (toRemoveTop > 0) {
                    dataItemList.remove(0, toRemoveTop);
                }
                dataItemList.addAll(messages);
                if (this.filteredItemList.size() > MAX_ENTRIES) {
                    int toRemove = dataItemList.size() - MAX_ENTRIES;
                    dataItemList.remove(0, toRemove);
                }
                this.dataItemTableView.scrollTo(0);
                this.dataItemTableView.refresh();
                updateSelectTime();
            }
        });
    }

    protected final void startSubscription() {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
        	this.delegator.resume();
            try {
                doServiceSubscribe(getCurrentFilter());
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        });
    }

    protected final void stopSubscription() {
        ReatmetricUI.threadPool(getClass()).execute(() -> {
        	try {
                doServiceUnsubscribe();
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        	this.delegator.suspend();
        });
    }

    protected void updateSelectTime() {
    	if(this.selectTimeBtn == null) {
    		return;
    	}
        // Take the first item from the table and use the generation time as value of the text
        if (filteredItemList.isEmpty()) {
            this.selectTimeBtn.setText("---");
        } else {
            T om = filteredItemList.get(0);
            Instant time = doGetGenerationTime(om);
            this.selectTimeBtn.setText(formatTime(time));
            this.dateTimePickerController.setSelectedTime(time);
        }
    }

    protected void clearTable() {
        dataItemList.clear();
        this.dataItemTableView.layout();
        this.dataItemTableView.refresh();
        updateSelectTime();
    }

    private void moveToTime(Instant selectedTime, RetrievalDirection direction, int n, V currentFilter) {
    	if(this.selectTimeBtn != null) {
    		this.selectTimeBtn.setText(formatTime(selectedTime));
    	}
    	
        markProgressBusy();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                List<T> messages = doRetrieve(selectedTime, n, direction, currentFilter);
                addDataItemsBack(messages, n, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            markProgressReady();
        });
    }

    private void markProgressBusy() {
        this.progressIndicator.setVisible(true);
    }

    private void markProgressReady() {
        FxUtils.runLater(() -> this.progressIndicator.setVisible(false));
    }

    private boolean isProcessingAvailable() {
        return !this.progressIndicator.isVisible();
    }
    
    private T getFirst() {
        return this.dataItemTableView.getItems().get(0);
    }

    private T getLast() {
        return this.dataItemTableView.getItems().get(this.dataItemTableView.getItems().size() - 1);
    }

    private int getNumVisibleRow() {
        double h = this.dataItemTableView.getHeight();
        h -= 30; // Header 
        return (int) (h / this.dataItemTableView.getFixedCellSize()) + 1;
    }

    protected void applyFilter(V selectedFilter) {
        this.dataItemFilterController.setSelectedFilter(selectedFilter);
        // Apply the filter on the current table
        if(selectedFilter != null && !selectedFilter.isClear()) {
            this.filteredItemList.setPredicate(selectedFilter);
        } else {
            this.filteredItemList.setPredicate(p -> true);
        }
        if(this.liveTgl == null || this.liveTgl.isSelected()) {
            if(selectedFilter == null || selectedFilter.isClear()) {
                ReatmetricUI.threadPool(getClass()).execute(() -> {
                    try {
                        doServiceSubscribe(selectedFilter);
                        markFilterDeactivated();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                ReatmetricUI.threadPool(getClass()).execute(() -> {
                    try {
                        doServiceSubscribe(selectedFilter);
                        markFilterActivated();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } else {
            if(selectedFilter == null || selectedFilter.isClear()) {
                markFilterDeactivated();
            } else {
                markFilterActivated();
            }
            moveToTime(this.dateTimePickerController.getSelectedTime(), RetrievalDirection.TO_PAST, getNumVisibleRow(), selectedFilter);
        }
    }

    private void markFilterDeactivated() {
        this.filterBtn.setStyle("");
    }

    private void markFilterActivated() {
        this.filterBtn.setStyle("-fx-background-color: -fx-faint-focus-color");
    }
   
    @Override
    protected Control doBuildNodeForPrinting() {
        return TableViewUtil.buildNodeForPrinting(this.dataItemTableView);
    }
    
    protected V getCurrentFilter() {
    	return this.dataItemFilterController != null ? this.dataItemFilterController.getSelectedFilter() : null; 
    }

    @Override
    public void dispose() {
        stopSubscription();
        super.dispose();
    }

    @Override
    protected Window retrieveWindow() {
        return displayTitledPane.getScene().getWindow();
    }

    protected abstract void doServiceSubscribe(V selectedFilter) throws ReatmetricException;
    
    protected abstract void doServiceUnsubscribe() throws ReatmetricException;
    
    protected abstract List<T> doRetrieve(T om, int n, RetrievalDirection direction, V filter) throws ReatmetricException;

    protected abstract List<T> doRetrieve(Instant selectedTime, int n, RetrievalDirection direction, V filter) throws ReatmetricException;

    protected abstract Instant doGetGenerationTime(T om);

    protected abstract URL doGetFilterWidget();
    
    protected abstract String doGetComponentId();
    
}

