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
import eu.dariolucia.reatmetric.api.activity.ActivityDescriptor;
import eu.dariolucia.reatmetric.api.activity.ActivityRouteState;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.*;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.ActivityDialogUtil;
import eu.dariolucia.reatmetric.ui.utils.DataProcessingDelegator;
import eu.dariolucia.reatmetric.ui.utils.DialogUtils;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.textfield.CustomTextField;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class ModelBrowserViewController extends AbstractDisplayController implements ISystemModelSubscriber {

    private static final Logger LOG = Logger.getLogger(ModelBrowserViewController.class.getName());

    private final Image containerImage = new Image(getClass().getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/elements_obj.gif"));
    private final Image parameterImage = new Image(getClass().getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/genericvariable_obj.gif"));
    private final Image eventImage = new Image(getClass().getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/smartmode_co.gif"));
    private final Image activityImage = new Image(getClass().getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/debugt_obj.gif"));
    private final Image reportImage = new Image(getClass().getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/file_obj.gif"));

    // Pane control
    @FXML
    private TitledPane displayTitledPane;
    
    @FXML
    private CustomTextField filterText;
    @FXML    
    private TreeTableView<SystemEntity> modelTree;
    @FXML    
    private TreeTableColumn<SystemEntity, String> nameCol;
    @FXML    
    private TreeTableColumn<SystemEntity, Status> statusCol;
    @FXML
    private ContextMenu contextMenu;



    private final Lock mapLock = new ReentrantLock();
    private final Map<SystemEntityPath, FilterableTreeItem<SystemEntity>> path2item = new TreeMap<>();
    private SystemEntity root = null;
    
    // Temporary message queue
    private DataProcessingDelegator<SystemEntity> delegator;
    private PopOver activityPopOver;

    @FXML
    private MenuItem expandAllMenuItem;
    @FXML
    private MenuItem collapseAllMenuItem;
    @FXML
    private SeparatorMenuItem expandCollapseSeparator;
    @FXML
    private SeparatorMenuItem executeActivitySeparator;
    @FXML
    private MenuItem executeActivityMenuItem;

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

    @FXML
    private void enableItemAction(ActionEvent event) {
        TreeItem<SystemEntity> se = this.modelTree.getSelectionModel().getSelectedItem();
        if(se != null && se.getValue().getStatus() != Status.ENABLED) {
            ReatmetricUI.threadPool(getClass()).execute(() -> {
                try {
                    ReatmetricUI.selectedSystem().getSystem().getSystemModelMonitorService().enable(se.getValue().getPath());
                } catch (ReatmetricException e) {
                    LOG.log(Level.SEVERE, "Problem while enabling system entity " + se.getValue().getPath() + ": " + e.getMessage(), e);
                }
            });
        }
    }

    @FXML
    private void disableItemAction(ActionEvent event) {
        TreeItem<SystemEntity> se = this.modelTree.getSelectionModel().getSelectedItem();
        if(se != null && se.getValue().getStatus() != Status.DISABLED) {
            ReatmetricUI.threadPool(getClass()).execute(() -> {
                try {
                    ReatmetricUI.selectedSystem().getSystem().getSystemModelMonitorService().disable(se.getValue().getPath());
                } catch (ReatmetricException e) {
                    LOG.log(Level.SEVERE, "Problem while disabling system entity " + se.getValue().getPath() + ": " + e.getMessage(), e);
                }
            });
        }
    }

    @FXML
    private void expandItemAction(ActionEvent event) {
        TreeItem<SystemEntity> se = this.modelTree.getSelectionModel().getSelectedItem();
        if(se != null) {
            expandItem(se, true);
        }
    }

    @FXML
    private void collapseItemAction(ActionEvent event) {
        TreeItem<SystemEntity> se = this.modelTree.getSelectionModel().getSelectedItem();
        if(se != null) {
            expandItem(se, false);
        }
    }

    private void expandItem(TreeItem<SystemEntity> item, boolean expand) {
        item.expandedProperty().set(expand);
        for(TreeItem<SystemEntity> child : item.getChildren()) {
            expandItem(child, expand);
        }
    }

    @Override
    protected Window retrieveWindow() {
        return displayTitledPane.getScene().getWindow();
    }

    @Override
    protected void doInitialize(URL url, ResourceBundle rb) {
        ImageView clearButton = new ImageView(new Image(getClass().getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/clear_input.png")));
        clearButton.setStyle("-fx-cursor: hand");
        clearButton.setOnMouseClicked(this::filterClearButtonPressed);
        this.filterText.setRight(clearButton);
        filterText.textProperty().addListener((obs, oldValue, newValue) -> {
            updatePredicate(newValue);
        });

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
                            break;
                        case DISABLED:
                            setTextFill(Color.DARKGRAY);
                            break;
                        case UNKNOWN:
                            setTextFill(Color.DARKORANGE);
                            break;
                        default:
                            setText("");
                            setTextFill(Color.BLACK);
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

        activityPopOver = new PopOver();
        activityPopOver.setHideOnEscape(true);
        activityPopOver.setAutoHide(false);
        activityPopOver.setDetachable(true);
    }

    private void updatePredicate(String newValue) {
        if(modelTree.getRoot() == null) {
            return;
        }
        if(newValue.isBlank()) {
            ((FilterableTreeItem<SystemEntity>) modelTree.getRoot()).predicateProperty().setValue(p -> true);
        } else {
            ((FilterableTreeItem<SystemEntity>) modelTree.getRoot()).predicateProperty().setValue(p -> p.getPath().asString().contains(newValue));
        }
    }

    @Override
    protected Control doBuildNodeForPrinting() {
        // Print function not available in this view
        return null;
    }

    @Override
    protected void doSystemDisconnected(IReatmetricSystem system, boolean oldStatus) {
        this.displayTitledPane.setDisable(true);
        // Clear the table
        clearTreeModel();
    }

    @Override
    protected void doSystemConnected(IReatmetricSystem system, boolean oldStatus) {
        this.displayTitledPane.setDisable(false);
        startSubscription();
    }

    private void startSubscription() {
        clearTreeModel();
        ReatmetricUI.threadPool(getClass()).execute(() -> {
            try {
                buildTreeModel();
            } catch (ReatmetricException e) {
                LOG.log(Level.SEVERE, "Problem while building tree model: " + e.getMessage(), e);
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
        FilterableTreeItem<SystemEntity> item = this.path2item.get(toAdd.getPath());
        if(item == null) {
            // Create the item
            item = new FilterableTreeItem<>(toAdd); //, new ImageView(getImageFromType(toAdd.getType())));
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

    private void addToParent(TreeItem<SystemEntity> item) {
        SystemEntityPath parentPath = item.getValue().getPath().getParent();
        if(parentPath == null) {
            // This is the root, do not do anything
            return;
        }
        FilterableTreeItem<SystemEntity> parent = this.path2item.get(parentPath);
        if(parent == null) {
            LOG.log(Level.WARNING, "Parent entity at path " + parentPath + " while adding " + item.getValue().getPath() + " not found: object skipped");
        } else {
            parent.getSourceChildren().add(item);
        }
    }

    private void addDataItems(List<SystemEntity> objects) {
        Platform.runLater(() -> {
            this.mapLock.lock();
            try {
                objects.forEach(this::addOrUpdateItemToTree);
                // this.modelTree.layout();
                this.modelTree.refresh();
            } finally {
                this.mapLock.unlock();
            }
        });
    }

    @FXML
    public void menuAboutToShow(WindowEvent windowEvent) {
        TreeItem<SystemEntity> selected = this.modelTree.getSelectionModel().getSelectedItem();
        if(selected == null || selected.getValue() == null) {
            return;
        }
        // Expand/collapse behaviour
        boolean showExpandCollapse = !selected.isLeaf();
        collapseAllMenuItem.setVisible(showExpandCollapse);
        expandAllMenuItem.setVisible(showExpandCollapse);
        expandCollapseSeparator.setVisible(showExpandCollapse);

        // Execute activity
        boolean showActivity = selected.getValue().getType() == SystemEntityType.ACTIVITY;
        executeActivitySeparator.setVisible(showActivity);
        executeActivityMenuItem.setVisible(showActivity);
    }

    @FXML
    private void executeActivityAction(ActionEvent actionEvent) {
        TreeItem<SystemEntity> selected = this.modelTree.getSelectionModel().getSelectedItem();
        if(selected == null || selected.getValue() == null || selected.getValue().getType() != SystemEntityType.ACTIVITY) {
            return;
        }
        try {
            // Get the descriptor
            AbstractSystemEntityDescriptor descriptor = ReatmetricUI.selectedSystem().getSystem().getSystemModelMonitorService().getDescriptorOf(selected.getValue().getExternalId());
            if (descriptor instanceof ActivityDescriptor) {
                // Get the route list
                List<ActivityRouteState> routeList = ReatmetricUI.selectedSystem().getSystem().getActivityExecutionService().getRouteAvailability(((ActivityDescriptor) descriptor).getActivityType());
                Pair<Node, ActivityInvocationDialogController> activityDialogPair = ActivityDialogUtil.createActivityInvocationDialog((ActivityDescriptor) descriptor, routeList);
                // Create the popup
                activityPopOver.setTitle("Execute activity " + descriptor.getPath().asString());
                activityPopOver.setContentNode(activityDialogPair.getFirst());
                activityDialogPair.getSecond().registerHandlers(this::runActivity, this::closeActivityPopOver);
                activityPopOver.show(modelTree);
            }
        } catch (IOException | ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot complete the requested operation: " + e.getMessage(), e);
        }
    }

    private void closeActivityPopOver(ActivityInvocationDialogController activityInvocationDialogController) {
        activityPopOver.hide();
    }

    private void runActivity(ActivityInvocationDialogController activityInvocationDialogController) {
        ActivityRequest request = activityInvocationDialogController.buildRequest();
        boolean confirm = DialogUtils.confirm("Request execution of activity", activityInvocationDialogController.getPath(), "Do you want to dispatch the execution request to the processing model?");
        if(confirm) {
            // Hide the popup
            activityPopOver.hide();
            // TODO: store activity request in activity invocation cache, to be used to initialise the same activity invocation in the future
            ReatmetricUI.threadPool(getClass()).execute(() -> {
                try {
                    ReatmetricUI.selectedSystem().getSystem().getActivityExecutionService().startActivity(request);
                } catch (ReatmetricException e) {
                    LOG.log(Level.SEVERE, "Cannot complete the requested operation: " + e.getMessage(), e);
                }
            });
        }
    }

    /*
     * Stack Overflow snippet from: https://stackoverflow.com/questions/15897936/javafx-2-treeview-filtering/34426897#34426897
     *
     * Thanks to kaznovac (https://stackoverflow.com/users/382655/kaznovac)
     */
    public static class FilterableTreeItem<T> extends TreeItem<T> {
        private final ObservableList<TreeItem<T>> sourceChildren = FXCollections.observableArrayList();
        private final FilteredList<TreeItem<T>> filteredChildren = new FilteredList<>(sourceChildren);
        private final ObjectProperty<Predicate<T>> predicate = new SimpleObjectProperty<>();

        public FilterableTreeItem(T value) {
            super(value);

            filteredChildren.predicateProperty().bind(Bindings.createObjectBinding(() -> {
                return child -> {
                    if (child instanceof FilterableTreeItem) {
                        ((FilterableTreeItem<T>) child).predicateProperty().set(predicate.get());
                    }
                    if (predicate.get() == null || !child.getChildren().isEmpty()) {
                        return true;
                    }
                    return predicate.get().test(child.getValue());
                };
            } , predicate));

            filteredChildren.addListener((ListChangeListener<TreeItem<T>>) c -> {
                while (c.next()) {
                    getChildren().removeAll(c.getRemoved());
                    getChildren().addAll(c.getAddedSubList());
                }
            });
        }

        public ObservableList<TreeItem<T>> getSourceChildren() {
            return sourceChildren;
        }

        public ObjectProperty<Predicate<T>> predicateProperty() {
            return predicate;
        }
    }
    /*
     * End of Stack Overflow snippet
     */
}
