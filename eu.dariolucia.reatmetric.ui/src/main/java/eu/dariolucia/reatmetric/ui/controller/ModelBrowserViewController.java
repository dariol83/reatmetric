/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import eu.dariolucia.reatmetric.api.common.ServiceType;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.ISystemModelSubscriber;
import eu.dariolucia.reatmetric.api.model.Status;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.model.SystemEntityUpdate;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.DataProcessingDelegator;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;

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
    private final Image referenceImage = new Image(getClass().getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/inst_ptr.gif"));
    
    
    
    // Pane control
    @FXML
    protected TitledPane displayTitledPane;
    
    @FXML
    private TextField filterText;
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
    private DataProcessingDelegator<SystemEntityUpdate> delegator;
    
    @FXML
    public void filterClearButtonPressed(ActionEvent e) {
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
        this.nameCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getValue().getName()));
        this.statusCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getValue().getStatus()));
        
        this.statusCol.setCellFactory(column -> {
            return new TreeTableCell<SystemEntity, Status>() {
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
                            case ABSENT:
                                setTextFill(Color.GRAY);
                                setStyle("-fx-font-weight: bold");
                                // setStyle("-fx-font-weight: bold -fx-background-color: DarkSlateGray");
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
            };
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
    protected void doUserDisconnected(String system, String user) {
        this.displayTitledPane.setDisable(true);
    }

    @Override
    protected void doUserConnected(String system, String user) {
        this.displayTitledPane.setDisable(false);
    }

    @Override
    protected void doUserConnectionFailed(String system, String user, String reason) {
        this.displayTitledPane.setDisable(true);
    }

    @Override
    protected void doServiceDisconnected(boolean previousConnectionStatus) {
        // Clear the table                
        clearTreeModel();
    }

    @Override
    protected void doServiceConnected(boolean previousConnectionStatus) {
        startSubscription();
    }
    
    @Override
    protected ServiceType doGetSupportedService() {
        return ServiceType.SYSTEM_MODEL;
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
    public void dataItemsReceived(List<SystemEntityUpdate> objects) {
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
                    List<SystemEntity> children = ReatmetricUI.selectedSystem().getSystem().getSystemModelMonitorService().getChildren(toProcess.getPath());
                    if(children != null) {
                        constructionQueue.addAll(children);
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

    private void removeItemFromTree(SystemEntity toRemove) throws ReatmetricException {
        TreeItem<SystemEntity> item = this.path2item.get(toRemove.getPath());
        if(item != null) {
            // Remove from map
            this.path2item.remove(toRemove.getPath());
            // Remove from parent
            SystemEntityPath parentPath = item.getValue().getPath().getParent();
            TreeItem<SystemEntity> parent = this.path2item.get(parentPath);
            if(parent != null) {
                parent.getChildren().remove(item);
            }
        }
    }
    
    private TreeItem<SystemEntity> addOrUpdateItemToTree(SystemEntity toAdd) throws ReatmetricException {
        // If there is already an item in the map, update the item
        TreeItem<SystemEntity> item = this.path2item.get(toAdd.getPath());
        if(item == null) {
            // Create the item
            item = new TreeItem<>(toAdd, new ImageView(getImageFromType(toAdd.getType())));
            // Add it to the map
            this.path2item.put(toAdd.getPath(), item);
            // Add it to the parent tree item
            addToParent(item);
        } else {
            // Replace the system element
            item.setValue(toAdd);
        }
        return item;
    }

    private Image getImageFromType(SystemEntityType t) {
        switch(t) {
            case CONTAINER: return containerImage;
            case ACTIVITY: return activityImage;
            case EVENT: return eventImage;
            case PARAMETER: return parameterImage;
            case REFERENCE: return referenceImage;
            case REPORT: return reportImage;
            default: return null;
        }
    }
    
    private void addToParent(TreeItem<SystemEntity> item) throws ReatmetricException {
        SystemEntityPath parentPath = item.getValue().getPath().getParent();
        if(parentPath == null) {
            // This is the root, do not do anything
            return;
        }
        TreeItem<SystemEntity> parent = this.path2item.get(parentPath);
        if(parent == null) {
            // Create the parent recursively (fake)
            SystemEntity parentEntity = new SystemEntity(null, parentPath, parentPath.getLastPathElement(), Status.UNKNOWN, AlarmState.UNKNOWN, SystemEntityType.CONTAINER);
            parent = addOrUpdateItemToTree(parentEntity);
        }    
        parent.getChildren().add(item);
    }

    private void addDataItems(List<SystemEntityUpdate> objects) {
        Platform.runLater(() -> {
            this.mapLock.lock();
            try {
                for(SystemEntityUpdate seu : objects) {
                    try {
                        switch(seu.getUpdateType()) {
                            case ADDITION:
                            case UPDATE:
                                addOrUpdateItemToTree(seu.getElement());
                            case DELETION:
                                removeItemFromTree(seu.getElement());
                        }
                    } catch(ReatmetricException e) {
                        e.printStackTrace();
                    }
                }
                this.modelTree.layout();
                this.modelTree.refresh();
            } finally {
                this.mapLock.unlock();
            }
        });
    }
}
