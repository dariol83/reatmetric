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
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.DialogUtils;
import eu.dariolucia.reatmetric.ui.utils.PresetStorageManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
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
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class ParameterDisplayViewController extends AbstractDisplayController {

	private static final Logger LOG = Logger.getLogger(ParameterDisplayViewController.class.getName());

	private URL cssUrl;

	// Pane control
	@FXML
	protected TitledPane displayTitledPane;

	@FXML
	protected TabPane tabPane;

	@FXML
	protected MenuButton loadBtn;

	// Preset manager
	private final PresetStorageManager presetManager = new PresetStorageManager();

	@Override
	protected Window retrieveWindow() {
		return displayTitledPane.getScene().getWindow();
	}

	@Override
	public final void doInitialize(URL url, ResourceBundle rb) {
		this.loadBtn.setOnShowing(this::onShowingPresetMenu);
		this.cssUrl = getClass().getClassLoader()
				.getResource("eu/dariolucia/reatmetric/ui/fxml/css/MainView.css");
	}

	protected String doGetComponentId() {
        return "ParameterDataView";
    }
    
	@FXML
	protected void newButtonSelected(ActionEvent e) throws IOException {
		createNewTab("Display");
	}

	private ParameterDisplayTabWidgetController createNewTab(String tabText) throws IOException {
		Tab t = new Tab(tabText);

		URL userDisplayWidgetUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/ParameterDisplayTabWidget.fxml");
		FXMLLoader loader = new FXMLLoader(userDisplayWidgetUrl);
		final VBox widget = loader.load();
		final ParameterDisplayTabWidgetController ctrl = loader.getController();

		widget.prefWidthProperty().bind(this.tabPane.widthProperty());
		t.setContent(widget);
		t.setClosable(true);
		t.setOnCloseRequest(event -> {
			if(DialogUtils.confirm("Close AND tab", "About to close AND tab " + t.getText(), "Do you want to close AND tab " + t.getText() + "? Unsaved AND updates will be lost!")) {
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

		t.setContextMenu(new ContextMenu());
		MenuItem renameTabMenuItem = new MenuItem("Rename...");
		t.getContextMenu().getItems().add(renameTabMenuItem);
		renameTabMenuItem.setOnAction(event -> {
			// Traditional way to get the response value.
			Optional<String> result = DialogUtils.input(t.getText(), "Rename Tab", "Change name of the chart tab", "Please provide the name of the chart tab:");
			result.ifPresent(t::setText);
		});
		MenuItem saveTabMenuItem = new MenuItem("Save chart preset...");
		t.getContextMenu().getItems().add(saveTabMenuItem);
		saveTabMenuItem.setOnAction(event -> {
			// Traditional way to get the response value.
			Optional<String> result = DialogUtils.input(t.getText(), "Save AND Preset", "AND Preset", "Please provide the name of the preset:");
			result.ifPresent(s -> this.presetManager.save(system.getName(), user, s, doGetComponentId(), ctrl.getParameterDisplayDescription()));
		});
		// Tab detaching
		SeparatorMenuItem sep = new SeparatorMenuItem();
		t.getContextMenu().getItems().add(sep);
		MenuItem detachMenuItem = new MenuItem("Detach");
		t.getContextMenu().getItems().add(detachMenuItem);
		detachMenuItem.setOnAction(event -> {
			// Create a detached scene parent
			Stage stage = new Stage();
			t.setContent(null);
			t.setOnCloseRequest(null);
			this.tabPane.getTabs().remove(t);
			// this.tab2contents.remove(t); // if removed, there will be no forwards of system status change
			Scene scene = new Scene(widget, 800, 600);
			scene.getStylesheets().add(cssUrl.toExternalForm());

			stage.setScene(scene);
			stage.setTitle(t.getText());

			Image icon = new Image(ReatmetricUI.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/logos/logo-small-color-32px.png"));
			stage.getIcons().add(icon);
			ctrl.setIndependentStage(stage);
			stage.setOnCloseRequest(ev -> {
				if(DialogUtils.confirm("Close chart", "About to close chart " + stage.getTitle(), "Do you want to close chart " + stage.getTitle() + "? Unsaved chart updates will be lost!")) {
					ctrl.dispose();
					stage.close();
				} else {
					ev.consume();
				}
			});

			stage.show();
		});

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
		this.loadBtn.getItems().remove(0, this.loadBtn.getItems().size());
		List<String> presets = this.presetManager.getAvailablePresets(system.getName(), user, doGetComponentId());
		for(String preset : presets) {
			final String fpreset = preset;
			MenuItem mi = new MenuItem(preset);
			mi.setOnAction((event) -> {
				Properties p = this.presetManager.load(system.getName(), user, fpreset, doGetComponentId());
				if(p != null) {
					try {
						addChartTabFromPreset(fpreset, p);
					} catch (IOException e) {
						LOG.log(Level.WARNING, "Cannot initialise chart tab preset " + preset + ": " + e.getMessage(), e);
					}
				}
			});
			this.loadBtn.getItems().add(mi);
		}
	}

	private void addChartTabFromPreset(String tabName, Properties p) throws IOException {
		final ParameterDisplayTabWidgetController t = createNewTab(tabName);
		// After adding the tab, you need to initialise it in a new round of the UI thread,
		// to allow the layouting of the tab and the correct definition of the parent elements,
		// hence avoiding a null pointer exception
		Platform.runLater(() -> t.addItemsFromPreset(p));
	}
}