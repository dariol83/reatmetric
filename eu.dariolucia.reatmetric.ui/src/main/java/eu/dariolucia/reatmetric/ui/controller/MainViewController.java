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
import eu.dariolucia.reatmetric.ui.utils.FxUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.StackPane;
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
public class MainViewController implements Initializable, IReatmetricServiceListener {

	private static final Logger LOG = Logger.getLogger(MainViewController.class.getName());

	private static final String NULL_PERSPECTIVE = "nullPerspective";

	private final ReatmetricPluginInspector serviceInspector = new ReatmetricPluginInspector();

	@FXML
	private RadioMenuItem parameterTgl;
	@FXML
	private RadioMenuItem parameterLogTgl;
	@FXML
	private RadioMenuItem eventTgl;
	@FXML
	private RadioMenuItem userDisplaysTgl;
	@FXML
	private RadioMenuItem alarmsTgl;
	@FXML
	private RadioMenuItem rawDataTgl;
	@FXML
	private RadioMenuItem mimicsTgl;
	@FXML
	private StackPane perspectiveStackPane;

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
	private MenuItem connectMenuItem;
	@FXML
	private MenuItem disconnectMenuItem;
	@FXML
	private MenuItem aboutMenuItem;
	@FXML
	private MenuItem exitMenuItem;

	/**
	 * Map the toggle button to the perspective to activate (by fx:id)
	 */
	private Map<RadioMenuItem, String> perspectiveMap = new HashMap<>();

	@FXML
	private void menubarViewAction(ActionEvent event) {
		RadioMenuItem source = (RadioMenuItem) event.getSource();
		if (source.isSelected()) {
			activatePerspective(this.perspectiveMap.get(source));
		} else {
			activatePerspective(NULL_PERSPECTIVE);
		}
	}

	private void activatePerspective(String perspectiveId) {
		if (perspectiveId == null) {
			LOG.log(Level.SEVERE, "Perspective null");
			return;
		}
		Node found = null;
		for (Node n : this.perspectiveStackPane.getChildren()) {
			if (n.getId() != null && n.getId().equals(perspectiveId)) {
				found = n;
				break;
			}
		}
		if (found != null) {
			found.toFront();
		} else {
			LOG.log(Level.SEVERE, "Perspective not found: " + perspectiveId);
		}
	}

	@FXML
	private void connectMenuAction(ActionEvent event) {
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
						return new String[] { systemCombo.getValue() };
					} else {
						return new String[] {};
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
				connectMenuItem.setDisable(true);
				ReatmetricUI.threadPool(getClass()).execute(() -> {
					ReatmetricUI.selectedSystem().setSystem(this.serviceInspector.getSystem(strings[0]));
				});
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void disconnectMenuAction(ActionEvent event) {
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

	@FXML
	public void menubarAboutAction(ActionEvent actionEvent) {
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

	@FXML
	private void exitMenuAction(ActionEvent event) {
		ReatmetricUI.shutdown();
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		FxUtils.setMenuItemImage(aboutMenuItem,"/eu/dariolucia/reatmetric/ui/fxml/images/16px/info.svg.png");
		FxUtils.setMenuItemImage(exitMenuItem,"/eu/dariolucia/reatmetric/ui/fxml/images/16px/power.svg.png");
		FxUtils.setMenuItemImage(connectMenuItem,"/eu/dariolucia/reatmetric/ui/fxml/images/16px/plug-f.svg.png");
		FxUtils.setMenuItemImage(disconnectMenuItem,"/eu/dariolucia/reatmetric/ui/fxml/images/16px/plug.svg.png");

		this.perspectiveMap.put(this.alarmsTgl, "alarmPerspective");
		this.perspectiveMap.put(this.rawDataTgl, "rawDataPerspective");
		this.perspectiveMap.put(this.mimicsTgl, "mimicsDisplayPerspective");
		this.perspectiveMap.put(this.parameterTgl, "monitoringPerspective");
		this.perspectiveMap.put(this.parameterLogTgl, "parameterLogPerspective");
		this.perspectiveMap.put(this.eventTgl, "eventPerspective");
		this.perspectiveMap.put(this.userDisplaysTgl, "userDisplayPerspective");
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

	private void disableMainViewItems() {
		// Disable the perspective buttons
		this.alarmsTgl.setDisable(true);
		this.rawDataTgl.setDisable(true);
		this.parameterTgl.setDisable(true);
		this.mimicsTgl.setDisable(true);
		this.parameterLogTgl.setDisable(true);
		this.eventTgl.setDisable(true);
		this.userDisplaysTgl.setDisable(true);
		// Disable the logout menu item and enable the other one
		this.disconnectMenuItem.setDisable(true);
		this.connectMenuItem.setDisable(false);
		// Disable the system label
		this.systemLbl.setText("---");
		this.systemLbl.setDisable(true);
		// Set the null perspective
		activatePerspective(NULL_PERSPECTIVE);
	}

	private void enableMainViewItems() {
		// Enable the perspective buttons
		this.alarmsTgl.setDisable(false);
		this.rawDataTgl.setDisable(false);
		this.parameterTgl.setDisable(false);
		this.mimicsTgl.setDisable(false);
		this.parameterLogTgl.setDisable(false);
		this.eventTgl.setDisable(false);
		this.userDisplaysTgl.setDisable(false);
		// Enable the logout menu item and enable the other one
		this.disconnectMenuItem.setDisable(false);
		this.connectMenuItem.setDisable(true);
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
