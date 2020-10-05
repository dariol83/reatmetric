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
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.DialogUtils;
import eu.dariolucia.reatmetric.ui.utils.PresetStorageManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class UserDisplayViewController extends AbstractDisplayController {

    private static final Logger LOG = Logger.getLogger(UserDisplayViewController.class.getName());

    private URL cssUrl;

    // Pane control
    @FXML
    protected TitledPane displayTitledPane;

    @FXML
    protected TabPane tabPane;

    @FXML
    protected MenuButton loadBtn;

    // Tab buttons
    @FXML
    protected Button detachButton;

    @FXML
    protected Button saveButton;

    @FXML
    protected Button renameButton;

    // Preset manager
    private final PresetStorageManager presetManager = new PresetStorageManager();

    // Avoid dialog when closing tabs due to parent close
    private boolean parentAboutToClose = false;

    @Override
    protected Window retrieveWindow() {
        return displayTitledPane.getScene().getWindow();
    }

    @Override
    public final void doInitialize(URL url, ResourceBundle rb) {
        this.loadBtn.setOnShowing(this::onShowingPresetMenu);
        this.cssUrl = getClass().getClassLoader()
                .getResource("eu/dariolucia/reatmetric/ui/fxml/css/MainView.css");

        // tab buttons visible/invisible depending on tab selection
        detachButton.setVisible(false);
        renameButton.setVisible(false);
        saveButton.setVisible(false);
        tabPane.getSelectionModel().selectedItemProperty().addListener((t) -> {
            detachButton.setVisible(!tabPane.getSelectionModel().isEmpty());
            renameButton.setVisible(!tabPane.getSelectionModel().isEmpty());
            saveButton.setVisible(!tabPane.getSelectionModel().isEmpty());
        });
    }

    protected String doGetComponentId() {
        return "ChartDisplayView";
    }

    @Override
    public void dispose() {
        parentAboutToClose = true;
        for (Tab tab : new ArrayList<>(tabPane.getTabs())) { // Avoid concurrent modification exceptions
            EventHandler<Event> handler = tab.getOnClosed();
            if (null != handler) {
                handler.handle(new Event(Tab.TAB_CLOSE_REQUEST_EVENT));
            } else {
                tab.getTabPane().getTabs().remove(tab);
            }
        }
        // Go up
        super.dispose();
    }

    @FXML
    protected void newButtonSelected(ActionEvent e) throws IOException {
        createNewTab("Display");
    }

    private UserDisplayTabWidgetController createNewTab(String tabText) throws IOException {
        Tab t = new Tab(tabText);

        URL userDisplayWidgetUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/UserDisplayTabWidget.fxml");
        FXMLLoader loader = new FXMLLoader(userDisplayWidgetUrl);
        final VBox userDisplayWidget = loader.load();
        final UserDisplayTabWidgetController ctrl = loader.getController();

        userDisplayWidget.prefWidthProperty().bind(this.tabPane.widthProperty());
        // userDisplayWidget.prefHeightProperty().bind(t.heightProperty()); // this creates problems with the height
        t.setContent(userDisplayWidget);
        t.setClosable(true);
        t.setOnCloseRequest(event -> {
            if (parentAboutToClose || DialogUtils.confirm("Close chart tab", "About to close chart tab " + t.getText(), "Do you want to close chart tab " + t.getText() + "? Unsaved chart updates will be lost!")) {
                this.tabPane.getTabs().remove(t);
                ctrl.dispose();
            } else {
                event.consume();
            }
        });
        this.tabPane.getTabs().add(t);
        this.tabPane.getParent().layout();
        this.tabPane.getSelectionModel().select(t);
        ctrl.startSubscription();
        t.setUserData(Pair.of(userDisplayWidget, ctrl));
        return ctrl;
    }

    @Override
    protected Control doBuildNodeForPrinting() {
        return null;
    }

    @Override
    protected void doSystemDisconnected(IReatmetricSystem system, boolean oldStatus) {
        this.displayTitledPane.setDisable(true);
    }

    @Override
    protected void doSystemConnected(IReatmetricSystem system, boolean oldStatus) {
        this.displayTitledPane.setDisable(false);
    }

    private void onShowingPresetMenu(Event contextMenuEvent) {
        String name;
        try {
            name = system.getName();
        } catch (RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot show presets, error contacting system", e);
            return;
        }

        this.loadBtn.getItems().remove(0, this.loadBtn.getItems().size());
        List<String> presets = this.presetManager.getAvailablePresets(name, user, doGetComponentId());
        for (String preset : presets) {
            final String fpreset = preset;
            MenuItem mi = new MenuItem(preset);
            mi.setOnAction((event) -> {
                if (!selectTab(fpreset)) {
                    loadPreset(name, fpreset);
                }
            });
            this.loadBtn.getItems().add(mi);
        }
    }

    private void loadPreset(String name, String fpreset) {
        Properties p = this.presetManager.load(name, user, fpreset, doGetComponentId());
        if (p != null) {
            try {
                addChartTabFromPreset(fpreset, p);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Cannot initialise tab preset " + fpreset + ": " + e.getMessage(), e);
            }
        }
    }

    private void addChartTabFromPreset(String tabName, Properties p) throws IOException {
        final UserDisplayTabWidgetController t = createNewTab(tabName);
        // After adding the tab, you need to initialise it in a new round of the UI thread,
        // to allow the layouting of the tab and the correct definition of the parent elements,
        // hence avoiding a null pointer exception
        Platform.runLater(() -> t.loadPreset(p));
    }

    public void open(String preset) {
        // Ugly but necessary
        Platform.runLater(() -> {
            ReatmetricUI.threadPool(getClass()).submit(() -> {
                String name;
                try {
                    name = system.getName();
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Cannot show presets, error contacting system", e);
                    return;
                }
                Platform.runLater(() -> {
                    if (!selectTab(preset)) {
                        loadPreset(name, preset);
                    }
                });
            });
        });
    }

    private boolean selectTab(String preset) {
        for (Tab t : tabPane.getTabs()) {
            if (t.getText().equals(preset)) {
                tabPane.getSelectionModel().select(t);
                return true;
            }
        }
        return false;
    }


    @FXML
    public void detachButtonClicked(ActionEvent actionEvent) {
        Tab t = this.tabPane.getSelectionModel().getSelectedItem();
        if(t == null) {
            return;
        }
        Pair<VBox, UserDisplayTabWidgetController> pair = (Pair<VBox, UserDisplayTabWidgetController>) t.getUserData();
        // Create a detached scene parent
        Stage stage = new Stage();
        t.setContent(null);
        t.setOnCloseRequest(null);
        this.tabPane.getTabs().remove(t);
        Scene scene = new Scene(pair.getFirst(), 800, 600);
        scene.getStylesheets().add(cssUrl.toExternalForm());

        stage.setScene(scene);
        stage.setTitle(t.getText());

        Image icon = new Image(ReatmetricUI.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/logos/logo-small-color-32px.png"));
        stage.getIcons().add(icon);
        pair.getSecond().setIndependentStage(stage);
        stage.setOnCloseRequest(ev -> {
            if(DialogUtils.confirm("Close", "About to close " + stage.getTitle(), "Do you want to close " + stage.getTitle() + "? Unsaved updates will be lost!")) {
                pair.getSecond().dispose();
                stage.close();
            } else {
                ev.consume();
            }
        });

        stage.show();
    }

    @FXML
    public void saveButtonClicked(ActionEvent actionEvent) {
        Tab t = this.tabPane.getSelectionModel().getSelectedItem();
        if(t == null) {
            return;
        }
        Pair<VBox, UserDisplayTabWidgetController> pair = (Pair<VBox, UserDisplayTabWidgetController>) t.getUserData();
        // Traditional way to get the response value.
        Optional<String> result = DialogUtils.input(t.getText(), "Save Preset", "Preset", "Please provide the name of the preset:");
        result.ifPresent(s -> {
            try {
                this.presetManager.save(system.getName(), user, s, doGetComponentId(), pair.getSecond().getChartDescription());
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Cannot save preset, system not responding", e);
            }
        });
    }

    @FXML
    public void renameButtonClicked(ActionEvent actionEvent) {
        Tab t = this.tabPane.getSelectionModel().getSelectedItem();
        if(t == null) {
            return;
        }
        // Traditional way to get the response value.
        Optional<String> result = DialogUtils.input(t.getText(), "Rename Tab", "Change name of the tab", "Please provide the name of the tab:");
        result.ifPresent(t::setText);
    }
}
