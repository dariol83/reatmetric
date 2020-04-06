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
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class MimicsDisplayViewController extends AbstractDisplayController {

	private static final Logger LOG = Logger.getLogger(MimicsDisplayViewController.class.getName());

	private static final String presetStorageLocation = System.getProperty("user.home") + File.separator + ReatmetricUI.APPLICATION_NAME + File.separator + "presets";

	// Pane control
	@FXML
	protected TitledPane displayTitledPane;

	@FXML
	protected TabPane tabPane;

	@FXML
	protected MenuButton loadBtn;

	private final Map<Tab, MimicsDisplayTabWidgetController> tab2contents = new ConcurrentHashMap<>();

	@Override
	protected Window retrieveWindow() {
		return displayTitledPane.getScene().getWindow();
	}

	@Override
	public final void doInitialize(URL url, ResourceBundle rb) {
		this.loadBtn.setOnShowing(this::onShowingPresetMenu);
	}

	protected String doGetComponentId() {
        return "MimicsDisplayView";
    }

	private Tab createNewTab(String tabText) throws IOException {
		Tab t = new Tab(tabText);

		URL mimicsDisplayWidgetUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/MimicsDisplayTabWidget.fxml");
		FXMLLoader loader = new FXMLLoader(mimicsDisplayWidgetUrl);
		VBox mimicsDisplayWidget = loader.load();
		MimicsDisplayTabWidgetController ctrl = loader.getController();

		mimicsDisplayWidget.prefWidthProperty().bind(this.tabPane.widthProperty());
		t.setContent(mimicsDisplayWidget);
		this.tabPane.getTabs().add(t);
		this.tabPane.getParent().layout();
		this.tabPane.getSelectionModel().select(t);
		this.tab2contents.put(t, ctrl);
		//
		if(system != null) {
			ctrl.systemConnected(system);
		} else {
			ctrl.systemDisconnected(system);
		}
		return t;
	}

	@FXML
	protected void closeButtonSelected(ActionEvent e) {
		Tab t = this.tabPane.getSelectionModel().getSelectedItem();
		if(t != null && DialogUtils.confirm("Close mimics tab", "About to close mimics tab " + t.getText(), "Do you want to close mimics tab " + t.getText() + "? Unsaved chart updates will be lost!")) {
			this.tabPane.getTabs().remove(t);
			this.tab2contents.remove(t).systemDisconnected(system);
		}
	}
	
	@Override
	protected Control doBuildNodeForPrinting() {
		return null;
	}

	@Override
	protected void doSystemDisconnected(IReatmetricSystem system, boolean oldStatus) {
		this.tab2contents.values().forEach(o -> o.systemDisconnected(system));
		this.displayTitledPane.setDisable(true);
	}

	@Override
	protected void doSystemConnected(IReatmetricSystem system, boolean oldStatus) {
		this.tab2contents.values().forEach(o -> o.systemConnected(system));
		this.displayTitledPane.setDisable(false);
	}

	private void onShowingPresetMenu(Event contextMenuEvent) {
		this.loadBtn.getItems().remove(0, this.loadBtn.getItems().size());
		List<String> presets = getAvailablePresets(system.getName(), user);
		for(String preset : presets) {
			final String fpreset = preset;
			MenuItem mi = new MenuItem(preset);
			mi.setOnAction((event) -> {
				File p = new File(presetStorageLocation + File.separator + system + File.separator + user + File.separator + doGetComponentId() + File.separator + fpreset + ".svg");
				if(p.exists() && p.canRead()) {
					try {
						addMimicsTabFromPreset(fpreset, p);
					} catch (IOException e) {
						LOG.log(Level.WARNING, "Cannot initialise mimics tab preset " + preset + ": " + e.getMessage(), e);
					}
				}
			});
			this.loadBtn.getItems().add(mi);
		}
	}

	public List<String> getAvailablePresets(String system, String user) {
		File folder = new File(presetStorageLocation + File.separator + system + File.separator + user + File.separator + doGetComponentId());
		if(!folder.exists()) {
			return Collections.emptyList();
		}
		List<String> presets = new LinkedList<>();
		for(File f : folder.listFiles()) {
			if(f.getName().endsWith("svg")) {
				presets.add(f.getName().substring(0, f.getName().length() - "svg".length() - 1));
			}
		}
		return presets;
	}

	private void addMimicsTabFromPreset(String name, File svgFile) throws IOException {
		Tab t = createNewTab(name);
		// After adding the tab, you need to initialise it in a new round of the UI thread,
		// to allow the layouting of the tab and the correct definition of the parent elements,
		// hence avoiding a null pointer exception
		Platform.runLater(() -> {
			MimicsDisplayTabWidgetController tabController = this.tab2contents.get(t);
			tabController.loadPreset(svgFile);
		});
	}
}
