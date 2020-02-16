/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.controller;

import eu.dariolucia.reatmetric.api.IServiceFactory;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.*;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.DataProcessingDelegator;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import org.controlsfx.control.textfield.CustomTextField;

import java.net.URL;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class ModelBrowserViewController extends AbstractDisplayController implements ISystemModelSubscriber {

    private final Image containerImage = new Image(getClass().getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/elements_obj.gif"));
    private final Image parameterImage = new Image(getClass().getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/genericvariable_obj.gif"));
    private final Image eventImage = new Image(getClass().getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/smartmode_co.gif"));
    private final Image activityImage = new Image(getClass().getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/debugt_obj.gif"));
    private final Image reportImage = new Image(getClass().getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/file_obj.gif"));

    // Pane control
    @FXML
    protected TitledPane displayTitledPane;
    
    @FXML
    private CustomTextField filterText;
    @FXML    
    private TreeTableView<SystemEntity> modelTree;
    @FXML    
    private TreeTableColumn<SystemEntity, String> nameCol;
    @FXML    
    private TreeTableColumn<SystemEntity, Status> statusCol;

    private final Lock mapLock = new ReentrantLock();
    private final Map<SystemEntityPath, TreeItem<SystemEntity>> path2item = new TreeMap<>();
    private SystemEntity root = null;
    
    // Temporary message queue
    private DataProcessingDelegator<SystemEntity> delegator;
    
    @FXML
    public void filterClearButtonPressed(Event e) {
        this.filterText.clear();
    }
    
    @FXML
    protected void onDragDetected(Event e) {
        /* drag was detected, start a drag-and-drop gesture*/
        /* allow any transfer mode */
        Dragboard db = this.modelTree.startDragAndDrop(TransferMode.ANY);
        
        final TreeItem<SystemEntity> selectedItem = this.modelTree.getSelectionModel().getSelectedItem();
        if(selectedItem != null && selectedItem.getValue() != null) {
            SystemEntity item = selectedItem.getValue();
            ClipboardContent content = new ClipboardContent();
            if(item.getType() == SystemEntityType.CONTAINER) {
                List<SystemEntity> list = new ArrayList<>();
                list.add(item);
                for(TreeItem<SystemEntity> child : selectedItem.getChildren()) {
                    list.add(child.getValue());
                }
                content.put(SystemEntityDataFormats.CONTAINER, list);
            } else {
                content.put(SystemEntityDataFormats.getByType(item.getType()), item);
            }
            db.setContent(content);
        }
        
        e.consume();
    }
    
    @Override
    protected void doInitialize(URL url, ResourceBundle rb) {
        ImageView clearButton = new ImageView(new Image(getClass().getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/clear_input.png")));
        clearButton.setStyle("-fx-cursor: hand");
        clearButton.setOnMouseClicked(this::filterClearButtonPressed);
        this.filterText.setRight(clearButton);

        this.nameCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getValue().getName()));
        this.nameCol.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty && !isEmpty()) {
                    setText(item);
                    switch (getTreeTableView().getTreeItem(getIndex()).getValue().getType()) {
                        case CONTAINER:
                            setGraphic(new ImageView(containerImage));
                            break;
                        case PARAMETER:
                            setGraphic(new ImageView(parameterImage));
                            break;
                        case EVENT:
                            setGraphic(new ImageView(eventImage));
                            break;
                        case ACTIVITY:
                            setGraphic(new ImageView(activityImage));
                            break;
                        case REPORT:
                            setGraphic(new ImageView(reportImage));
                            break;
                        default:
                            setGraphic(null);
                            break;
                    }
                } else {
                    setText("");
                    setGraphic(null);
                }
            }
        });
        this.statusCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getValue().getStatus()));
        this.statusCol.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(Status item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty && !isEmpty()) {
                    setText(item.name());
                    switch (item) {
                        case ENABLED:
                            setTextFill(Color.LIMEGREEN);
                            setStyle("-fx-font-weight: bold");
                            // setStyle("-fx-font-weight: bold; -fx-background-color: Lime");
                            break;
                        case DISABLED:
                            setTextFill(Color.DARKGRAY);
                            setStyle("-fx-font-weight: bold");
                            // setStyle("-fx-font-weight: bold; -fx-background-color: LightGray");
                            break;
                        case UNKNOWN:
                            setTextFill(Color.DARKORANGE);
                            setStyle("-fx-font-weight: bold");
                            // setStyle("-fx-font-weight: bold; -fx-background-color: DarkKhaki");
                            break;
                        default:
                            setText("");
                            setTextFill(Color.BLACK);
                            setStyle("");
                            break;
                    }
                } else {
                    setText("");
                    setGraphic(null);
                }
            }
        });
        
        this.delegator = new DataProcessingDelegator<>("Model Browser Delegator", (a) -> {
            this.mapLock.lock();
            try {
                addDataItems(a);
            } finally {
                this.mapLock.unlock();
            }
        });
    }

    @Override
    protected Control doBuildNodeForPrinting() {
        // Print function not available in this view
        return null;
    }

    @Override
    protected void doSystemDisconnected(IServiceFactory system, boolean oldStatus) {
        this.displayTitledPane.setDisable(true);
        // Clear the table
        clearTreeModel();
    }

    @Override
    protected void doSystemConnected(IServiceFactory system, boolean oldStatus) {
        this.displayTitledPane.setDisable(false);
        startSubscription();
    }

    private void startSubscription() {
        clearTreeModel();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                buildTreeModel();
            } catch (ReatmetricException e) {
                e.printStackTrace();
            }
        });
    }

    private void clearTreeModel() {
        // First lock
        this.mapLock.lock();
        try {
            this.root = null;
            this.path2item.clear();
        } finally {
            this.mapLock.unlock();
        }
        this.modelTree.setRoot(null);
        this.modelTree.layout();
        this.modelTree.refresh();
    }

    @Override
    public void dataItemsReceived(List<SystemEntity> objects) {
        this.delegator.delegate(objects);
    }

    private void buildTreeModel() throws ReatmetricException {
        // First lock
        this.mapLock.lock();
        try {
            // Unsubscribe if any
            ReatmetricUI.selectedSystem().getSystem().getSystemModelMonitorService().unsubscribe(this);
            // Subscribe to updates
            ReatmetricUI.selectedSystem().getSystem().getSystemModelMonitorService().subscribe(this);
            // Callbacks might come now, but they cannot update the model (TODO: make it better)
            
            // Get the root node
            this.root = ReatmetricUI.selectedSystem().getSystem().getSystemModelMonitorService().getRoot();
            Queue<SystemEntity> constructionQueue = new LinkedList<>();
            constructionQueue.add(this.root);
            while(!constructionQueue.isEmpty()) {
                SystemEntity toProcess = constructionQueue.poll();
                if(!this.path2item.containsKey(toProcess.getPath())) {
                    addOrUpdateItemToTree(toProcess);
                    if(toProcess.getType() == SystemEntityType.CONTAINER) {
                        List<SystemEntity> children = ReatmetricUI.selectedSystem().getSystem().getSystemModelMonitorService().getContainedEntities(toProcess.getPath());
                        if (children != null) {
                            constructionQueue.addAll(children);
                        }
                    }
                }
            }
            // Now you can ask the UI thread to set the root in the viewer
            Platform.runLater(() -> {
                this.modelTree.setRoot(this.path2item.get(this.root.getPath()));
                this.modelTree.layout();
                this.modelTree.refresh();
            });
        } finally {
            this.mapLock.unlock();
        }
    }

    private TreeItem<SystemEntity> addOrUpdateItemToTree(SystemEntity toAdd) {
        // If there is already an item in the map, update the item
        TreeItem<SystemEntity> item = this.path2item.get(toAdd.getPath());
        if(item == null) {
            // Create the item
            item = new TreeItem<>(toAdd); //, new ImageView(getImageFromType(toAdd.getType())));
            // Add it to the map
            this.path2item.put(toAdd.getPath(), item);
            // Add it to the parent tree item
            addToParent(item);
        } else {
            // Replace the system element, if the internal ID is more recent
            SystemEntity currentState = item.getValue();
            if(currentState.getInternalId().asLong() < toAdd.getInternalId().asLong()) {
                item.setValue(toAdd);
            }
        }
        return item;
    }

    private Image getImageFromType(SystemEntityType t) {
        switch(t) {
            case CONTAINER: return containerImage;
            case ACTIVITY: return activityImage;
            case EVENT: return eventImage;
            case PARAMETER: return parameterImage;
            case REPORT: return reportImage;
            default: return null;
        }
    }
    
    private void addToParent(TreeItem<SystemEntity> item) {
        SystemEntityPath parentPath = item.getValue().getPath().getParent();
        if(parentPath == null) {
            // This is the root, do not do anything
            return;
        }
        TreeItem<SystemEntity> parent = this.path2item.get(parentPath);
        if(parent == null) {
            // TODO: log problem
        } else {
            parent.getChildren().add(item);
        }
    }

    private void addDataItems(List<SystemEntity> objects) {
        Platform.runLater(() -> {
            this.mapLock.lock();
            try {
                objects.forEach(this::addOrUpdateItemToTree);
                this.modelTree.layout();
                this.modelTree.refresh();
            } finally {
                this.mapLock.unlock();
            }
        });
    }
}
