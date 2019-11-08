/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.controller;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import eu.dariolucia.reatmetric.api.IServiceFactory;
import eu.dariolucia.reatmetric.api.common.IUserMonitorCallback;
import eu.dariolucia.reatmetric.api.common.exceptions.MonitoringCentreException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.plugin.IMonitoringCentreServiceListener;
import eu.dariolucia.reatmetric.ui.plugin.MonitoringCentrePluginInspector;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

/**
 *
 * @author dario
 */
public class MainViewController implements Initializable, IMonitoringCentreServiceListener, IUserMonitorCallback {

	private static final String NULL_PERSPECTIVE = "nullPerspective";

	private final MonitoringCentrePluginInspector serviceInspector = new MonitoringCentrePluginInspector();

	@FXML
	private ToggleButton parameterTgl;
	@FXML
	private ToggleButton eventTgl;
	@FXML
	private ToggleButton userDisplaysTgl;
	@FXML
	private ToggleButton alarmsTgl;
	@FXML
	private ToggleButton rawDataTgl;
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
	private Label usernameLbl;
	@FXML
	private Label statusLbl;
	@FXML
	private Label systemTextLbl;
	@FXML
	private Label usernameTextLbl;

	@FXML
	private MenuItem connectMenuItem;
	@FXML
	private MenuItem disconnectMenuItem;

	/**
	 * Map the toggle button to the perspective to activate (by fx:id)
	 */
	private Map<ToggleButton, String> perspectiveMap = new HashMap<ToggleButton, String>();

	@FXML
	private void toolbarToggleButtonAction(ActionEvent event) {
		ToggleButton source = (ToggleButton) event.getSource();
		if (source.isSelected()) {
			activatePerspective(this.perspectiveMap.get(source));
		} else {
			activatePerspective(NULL_PERSPECTIVE);
		}
	}

	private void activatePerspective(String perspectiveId) {
		if (perspectiveId == null) {
			// TODO: error dialog box
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
			d.setHeaderText("Select the system and provide the related credential information");

			// Set the button types.
			ButtonType loginButtonType = new ButtonType("Login", ButtonData.OK_DONE);
			d.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

			Parent root = FXMLLoader.load(getClass().getClassLoader()
					.getResource("eu/dariolucia/reatmetric/ui/fxml/ConnectDialog.fxml"));
			d.getDialogPane().setContent(root);
			ComboBox<String> systemCombo = (ComboBox<String>) root.lookup("#systemCombo");
			TextField usernameText = (TextField) root.lookup("#usernameText");
			PasswordField passwordText = (PasswordField) root.lookup("#passwordText");
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
						return new String[] { systemCombo.getValue().toString(), usernameText.getText(),
								passwordText.getText() };
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
			// Request focus on the username field by default.
			Platform.runLater(() -> usernameText.requestFocus());

			Optional<String[]> data = d.showAndWait();
			if (data.isPresent()) {
				ReatmetricUI.selectedSystem().setSystem(this.serviceInspector.getSystem(data.get()[0]));
				ReatmetricUI.selectedSystem().getSystem().register(this);
				ReatmetricUI.selectedSystem().getSystem().login(data.get()[1], data.get()[2]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (MonitoringCentreException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void disconnectMenuAction(ActionEvent event) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Disconnect user");
		alert.setHeaderText("Disconnect user");
		String s = "Do you want to logout user " + this.usernameLbl.getText() + " from system "
				+ this.systemLbl.getText() + "?";
		alert.setContentText(s);
		alert.getDialogPane().getStylesheets().add(getClass().getClassLoader()
				.getResource("eu/dariolucia/reatmetric/ui/fxml/css/MainView.css").toExternalForm());
		Optional<ButtonType> result = alert.showAndWait();
		if ((result.isPresent()) && (result.get() == ButtonType.OK)) {
			ReatmetricUI.selectedSystem().getSystem().logout();
			ReatmetricUI.selectedSystem().getSystem().deregister(this);
			ReatmetricUI.selectedSystem().setSystem(null);
		}
	}

	@FXML
	private void exitMenuAction(ActionEvent event) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Exit Monitoring Centre UI");
		alert.setHeaderText("Exit Monitoring Centre UI");
		String s = "Do you want to close Monitoring Centre UI?";
		alert.setContentText(s);
		alert.getDialogPane().getStylesheets().add(getClass().getClassLoader()
				.getResource("eu/dariolucia/reatmetric/ui/fxml/css/MainView.css").toExternalForm());
		Optional<ButtonType> result = alert.showAndWait();
		if ((result.isPresent()) && (result.get() == ButtonType.OK)) {
			Platform.exit();
		}
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		this.perspectiveMap.put(this.alarmsTgl, "alarmPerspective");
		this.perspectiveMap.put(this.rawDataTgl, "rawDataPerspective");
		this.perspectiveMap.put(this.parameterTgl, "monitoringPerspective");
		this.perspectiveMap.put(this.eventTgl, "eventPerspective");
		this.perspectiveMap.put(this.userDisplaysTgl, "userDisplayPerspective");
		this.usernameTextLbl.disableProperty().bind(this.usernameLbl.disableProperty());
		this.systemTextLbl.disableProperty().bind(this.systemLbl.disableProperty());
		ReatmetricUI.selectedSystem().addSubscriber(this);
		// Register the status label
		ReatmetricUI.registerStatusLabel(this.statusLbl);
		ReatmetricUI.registerStatusIndicator(this::updateStatusIndicator);
	}

	@Override
	public void systemAdded(IServiceFactory system) {
		// Nothing to do yet
	}

	@Override
	public void systemRemoved(IServiceFactory system) {
		// Nothing to do yet
	}

	@Override
	public void userDisconnected(String system, String user) {
		Platform.runLater(() -> {
			disableMainViewItems();
			ReatmetricUI.setStatusLabel("User " + user + " disconnected");
		});
	}

	private void disableMainViewItems() {
		// Disable the perspective buttons
		this.alarmsTgl.setDisable(true);
		this.rawDataTgl.setDisable(true);
		this.parameterTgl.setDisable(true);
		this.eventTgl.setDisable(true);
		this.userDisplaysTgl.setDisable(true);
		// Disable the logout menu item and enable the other one
		this.disconnectMenuItem.setDisable(true);
		this.connectMenuItem.setDisable(false);
		// Disable the two labels
		this.systemLbl.setText("---");
		this.systemLbl.setDisable(true);
		this.usernameLbl.setText("---");
		this.usernameLbl.setDisable(true);
		// Set the null perspective
		activatePerspective(NULL_PERSPECTIVE);
	}

	@Override
	public void userConnected(String system, String user) {
		Platform.runLater(() -> {
			enableMainViewItems();
			this.systemLbl.setText(system);
			this.usernameLbl.setText(user);
			ReatmetricUI.setStatusLabel("User " + user + " connected to system " + system);
		});
	}

	private void enableMainViewItems() {
		// Enable the perspective buttons
		this.alarmsTgl.setDisable(false);
		this.rawDataTgl.setDisable(false);
		this.parameterTgl.setDisable(false);
		this.eventTgl.setDisable(false);
		this.userDisplaysTgl.setDisable(false);
		// Enable the logout menu item and enable the other one
		this.disconnectMenuItem.setDisable(false);
		this.connectMenuItem.setDisable(true);
		// Enable the two labels
		this.systemLbl.setDisable(false);
		this.usernameLbl.setDisable(false);
	}

	@Override
	public void userConnectionFailed(String system, String user, String reason) {
		Platform.runLater(() -> {
			disableMainViewItems();
			ReatmetricUI.setStatusLabel("User connection failed: " + reason);
		});
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
