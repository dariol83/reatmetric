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
import eu.dariolucia.reatmetric.ui.plugin.ReatmetricPluginInspector;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dario
 */
public class AlternativeMainViewController implements Initializable, IReatmetricServiceListener {

	private static final Logger LOG = Logger.getLogger(AlternativeMainViewController.class.getName());

	private static final String NULL_PERSPECTIVE = "nullPerspective";

	private static final String VIEW_LOCATION = "viewLocation";

	private final ReatmetricPluginInspector serviceInspector = new ReatmetricPluginInspector();

	@FXML
	private TabPane viewTabPane;
	@FXML
	private VBox buttonBox;

	private double mainSplitPanePositionBeforeCollapsing = 1.0;

	@FXML
	private Button monitoringViewButton;

	@FXML
	private Circle nominalCrl;
	@FXML
	private Circle warningCrl;
	@FXML
	private Circle alarmCrl;

	@FXML
	private Label systemLbl;
	@FXML
	private Label statusLbl;
	@FXML
	private ProgressBar globalProgress;

	@FXML
	private Button connectButton;
	@FXML
	private Button sideButton;
	@FXML
	private Button infoButton;

	/**
	 * Map the button to the view to activate (by fx:id)
	 */
	private Map<String, String> perspectiveMap = new HashMap<>();

	@FXML
	private void viewAction(ActionEvent event) {
		Button source = (Button) event.getSource();
		activatePerspective(source, this.perspectiveMap.get(source.getId()));
	}

	// TODO: redundnant arguments, simplify
	private void activatePerspective(Button viewButton, String perspectiveId) {
		if (perspectiveId == null) {
			LOG.log(Level.SEVERE, "Perspective null");
			return;
		}
		Tab found = null;
		for (Tab n : viewTabPane.getTabs()) {
			if (n.getId() != null && n.getId().equals(perspectiveId)) {
				found = n;
				break;
			}
		}
		if (found != null) {
			viewTabPane.getSelectionModel().select(found);
		} else {
			createView(viewButton, perspectiveId);
		}
	}

	private void createView(Button buttonView, String perspectiveId) {

		URL viewUrl = getClass().getResource(perspectiveId);
		FXMLLoader loader = new FXMLLoader(viewUrl);
		Node view = loader.load();
		AbstractDisplayController ctrl = loader.getController();

		Tab t = new Tab(buttonView.getText(), view);
		t.setId(perspectiveId);
		viewTabPane.getTabs().add(t);
		// TODO: add stop on tab close for any type of subscription in the controller
	}

	@FXML
	private void connectAction(ActionEvent event) {
		if(!ReatmetricUI.selectedSystem().isPresent()) {
			try {
				Dialog<String[]> d = new Dialog<>();
				d.setTitle("Connect to system");
				d.setHeaderText("Select the system to connect to");

				// Set the button types.
				ButtonType loginButtonType = new ButtonType("Connect", ButtonData.OK_DONE);
				d.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

				Parent root = FXMLLoader.load(getClass().getClassLoader()
						.getResource("eu/dariolucia/reatmetric/ui/fxml/ConnectDialog.fxml"));
				d.getDialogPane().setContent(root);
				ComboBox<String> systemCombo = (ComboBox<String>) root.lookup("#systemCombo");
				// TextField usernameText = (TextField) root.lookup("#usernameText");
				// PasswordField passwordText = (PasswordField) root.lookup("#passwordText");
				// First: let's retrieve the list of available systems and set them in the combo
				List<String> systems = serviceInspector.getAvailableSystems();
				systemCombo.setItems(FXCollections.observableList(systems));
				// Select the first one if any
				if (!systems.isEmpty()) {
					systemCombo.setValue(systems.get(0));
				} else {
					// Disable the login button if there is no system
					Node loginButton = d.getDialogPane().lookupButton(loginButtonType);
					loginButton.setDisable(true);
				}

				d.setResultConverter(buttonType -> {
					if (buttonType.equals(loginButtonType)) {
						if (systemCombo.getValue() != null) {
							return new String[]{systemCombo.getValue()};
						} else {
							return new String[]{};
						}
					} else {
						return null;
					}
				});
				// Set the CSS
				d.getDialogPane().getStylesheets().add(getClass().getClassLoader()
						.getResource("eu/dariolucia/reatmetric/ui/fxml/css/MainView.css").toExternalForm());

				Optional<String[]> data = d.showAndWait();
				// Start in a separate thread, as connection can take time
				data.ifPresent(strings -> {
					// Avoid that the user clicks again on the menu item
					connectButton.setDisable(true);
					ReatmetricUI.threadPool(getClass()).execute(() -> {
						try {
							ReatmetricUI.selectedSystem().setSystem(this.serviceInspector.getSystem(strings[0]));
						} catch (Exception e) {
							connectButton.setDisable(false);
						}
					});
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Disconnect user");
			alert.setHeaderText("Disconnect user");
			String s = "Do you want to disconnect from system "
					+ this.systemLbl.getText() + "?";
			alert.setContentText(s);
			alert.getDialogPane().getStylesheets().add(getClass().getClassLoader()
					.getResource("eu/dariolucia/reatmetric/ui/fxml/css/MainView.css").toExternalForm());
			Optional<ButtonType> result = alert.showAndWait();
			if ((result.isPresent()) && (result.get() == ButtonType.OK)) {
				ReatmetricUI.selectedSystem().setSystem(null);
			}
		}
	}

	@FXML
	public void aboutAction(ActionEvent actionEvent) {
		try {
			Dialog<Void> d = new Dialog<>();
			d.setTitle("About ReatMetric...");
			d.setHeaderText(null);

			// Set the button types.
			d.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

			Parent root = FXMLLoader.load(getClass().getClassLoader()
					.getResource("eu/dariolucia/reatmetric/ui/fxml/AboutDialog.fxml"));
			d.getDialogPane().setContent(root);
			// Set the CSS
			d.getDialogPane().getStylesheets().add(getClass().getClassLoader()
					.getResource("eu/dariolucia/reatmetric/ui/fxml/css/MainView.css").toExternalForm());

			d.showAndWait();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		// Read the views
		for(Node n : buttonBox.getChildren()) {
			if(n instanceof Button && n.getProperties().get(VIEW_LOCATION) != null) {
				perspectiveMap.put(n.getId(), n.getProperties().get(VIEW_LOCATION).toString());
			}
		}

		// TODO: hook width property of view button to vbox width

		// Add the subscriber
		ReatmetricUI.selectedSystem().addSubscriber(this);

		// Register the status label
		ReatmetricUI.registerStatusLabel(this.statusLbl);
	}

	@Override
	public void startGlobalOperationProgress() {
		Platform.runLater(() -> {
			this.globalProgress.setVisible(true);
			this.globalProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
		});
	}

	@Override
	public void stopGlobalOperationProgress() {
		Platform.runLater(() -> {
			this.globalProgress.setProgress(0);
			this.globalProgress.setVisible(false);
		});
	}

	@Override
	public void systemConnected(IReatmetricSystem system) {
		Platform.runLater(() -> {
			enableMainViewItems();
			this.systemLbl.setText(system.getName());
			ReatmetricUI.setStatusLabel("System " + system.getName() + " connected");
		});
	}

	@Override
	public void systemDisconnected(IReatmetricSystem system) {
		Platform.runLater(() -> {
			disableMainViewItems();
			ReatmetricUI.setStatusLabel("System " + system.getName() + " disconnected");
		});
	}

	@Override
	public void systemStatusUpdate(SystemStatus status) {
		Platform.runLater(() -> {
			updateStatusIndicator(status);
		});
	}

	/*
	 * StackOverflow method snippet: https://stackoverflow.com/questions/17047000/javafx-closing-a-tab-in-tabpane-dynamically
	 * Thanks to Daniel (https://stackoverflow.com/users/2837642/daniel)
	 */
	private void closeTab(Tab tab) {
		EventHandler<Event> handler = tab.getOnClosed();
		if (handler != null) {
			handler.handle(null);
		} else {
			tab.getTabPane().getTabs().remove(tab);
		}
	}

	private void disableMainViewItems() {
		// Close all tabs
		List.copyOf(viewTabPane.getTabs()).forEach(this::closeTab);

		// Disable the perspective buttons
		buttonBox.getChildren().forEach(o -> o.setDisable(true));

		// Disable the logout menu item and enable the other one
		// TODO: update button picture

		// Disable the system label
		this.systemLbl.setText("---");
		this.systemLbl.setDisable(true);
	}

	private void enableMainViewItems() {
		// Enable the perspective buttons
		buttonBox.getChildren().forEach(o -> o.setDisable(false));

		// Enable the logout menu item and enable the other one
		// TODO: update button picture

		// Enable the system label
		this.systemLbl.setDisable(false);
	}

	public void updateStatusIndicator(SystemStatus state) {
		Platform.runLater(() -> {
			switch (state) {
			case ALARM:
				this.nominalCrl.setFill(Paint.valueOf("#003915"));
				this.warningCrl.setFill(Paint.valueOf("#382700"));
				this.alarmCrl.setFill(Paint.valueOf("#CC0000"));
				break;
			case WARNING:
				this.nominalCrl.setFill(Paint.valueOf("#003915"));
				this.warningCrl.setFill(Paint.valueOf("#CCAA00"));
				this.alarmCrl.setFill(Paint.valueOf("#360000"));
				break;
			case NOMINAL:
				this.nominalCrl.setFill(Paint.valueOf("#00FF15"));
				this.warningCrl.setFill(Paint.valueOf("#382700"));
				this.alarmCrl.setFill(Paint.valueOf("#360000"));
				break;
			default:
				this.nominalCrl.setFill(Paint.valueOf("#003915"));
				this.warningCrl.setFill(Paint.valueOf("#382700"));
				this.alarmCrl.setFill(Paint.valueOf("#360000"));
				break;
			}
		});
	}

}
