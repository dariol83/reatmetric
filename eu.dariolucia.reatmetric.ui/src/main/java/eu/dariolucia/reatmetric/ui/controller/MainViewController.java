/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.controller;

import eu.dariolucia.reatmetric.api.IServiceFactory;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.plugin.IReatmetricServiceListener;
import eu.dariolucia.reatmetric.ui.plugin.ReatmetricPluginInspector;
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
 *
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
			LOG.log(Level.SEVERE, "Perspective not found");
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
	private void exitMenuAction(ActionEvent event) {
		ReatmetricUI.shutdown();
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.perspectiveMap.put(this.alarmsTgl, "alarmPerspective");
		this.perspectiveMap.put(this.rawDataTgl, "rawDataPerspective");
		this.perspectiveMap.put(this.parameterTgl, "monitoringPerspective");
		this.perspectiveMap.put(this.parameterLogTgl, "parameterLogPerspective");
		this.perspectiveMap.put(this.eventTgl, "eventPerspective");
		this.perspectiveMap.put(this.userDisplaysTgl, "userDisplayPerspective");
		ReatmetricUI.selectedSystem().addSubscriber(this);
		// Register the status label
		ReatmetricUI.registerStatusLabel(this.statusLbl);
		ReatmetricUI.registerStatusIndicator(this::updateStatusIndicator);
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
	public void systemConnected(IServiceFactory system) {
		Platform.runLater(() -> {
			enableMainViewItems();
			this.systemLbl.setText(system.getSystem());
			ReatmetricUI.setStatusLabel("System " + system.getSystem() + " connected");
		});
	}

	@Override
	public void systemDisconnected(IServiceFactory system) {
		Platform.runLater(() -> {
			disableMainViewItems();
			ReatmetricUI.setStatusLabel("System " + system.getSystem() + " disconnected");
		});
	}

	private void disableMainViewItems() {
		// Disable the perspective buttons
		this.alarmsTgl.setDisable(true);
		this.rawDataTgl.setDisable(true);
		this.parameterTgl.setDisable(true);
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
		this.parameterLogTgl.setDisable(false);
		this.eventTgl.setDisable(false);
		this.userDisplaysTgl.setDisable(false);
		// Enable the logout menu item and enable the other one
		this.disconnectMenuItem.setDisable(false);
		this.connectMenuItem.setDisable(true);
		// Enable the system label
		this.systemLbl.setDisable(false);
	}

	public void updateStatusIndicator(AlarmState state) {
		Platform.runLater(() -> {
			switch (state) {
			case ALARM:
			case ERROR:
				this.nominalCrl.setFill(Paint.valueOf("#003915"));
				this.warningCrl.setFill(Paint.valueOf("#382700"));
				this.alarmCrl.setFill(Paint.valueOf("#CC0000"));
				break;
			case WARNING:
			case VIOLATED:
				this.nominalCrl.setFill(Paint.valueOf("#003915"));
				this.warningCrl.setFill(Paint.valueOf("#CCAA00"));
				this.alarmCrl.setFill(Paint.valueOf("#360000"));
				break;
			default:
				this.nominalCrl.setFill(Paint.valueOf("#00FF15"));
				this.warningCrl.setFill(Paint.valueOf("#382700"));
				this.alarmCrl.setFill(Paint.valueOf("#360000"));
				break;
			}
		});
	}

}
