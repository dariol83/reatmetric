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
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.plugin.IReatmetricServiceListener;
import eu.dariolucia.reatmetric.ui.plugin.ReatmetricPluginInspector;
import eu.dariolucia.reatmetric.ui.utils.InstantCellFactory;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dario
 */
public class MainViewController implements Initializable, IReatmetricServiceListener {

    private static final Logger LOG = Logger.getLogger(MainViewController.class.getName());

    public static final String WARNING_COLOR = "#CCAA00";
    public static final String ALARM_COLOR = "#CC0000";
    public static final String NOMINAL_COLOR = "#00FF15";
    public static final String DISABLED_COLOR = "#003915";

    private final Image CONNECT_IMAGE = new Image(getClass().getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/48px/plug-f.svg.png"));
    private final Image DISCONNECT_IMAGE = new Image(getClass().getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/48px/plug.svg.png"));

    private static final String VIEW_LOCATION = "viewLocation";
    private static final String VIEW_NAME = "viewName";
    private static final String VIEW_IMAGE = "viewImage";

    private static final URL CSS_URL = MainViewController.class.getClassLoader().getResource("eu/dariolucia/reatmetric/ui/fxml/css/MainView.css");

    private static final String SYSTEM_LABEL_CSS_STYLE_NOALARM = "-fx-border-color: black; -fx-background-color: #c6c6c6; -fx-text-fill: #1a1a1a;";
    private static final String SYSTEM_LABEL_CSS_STYLE_ALARM = "-fx-border-color: black; -fx-background-color: #c60000; -fx-text-fill: #FFFFFF;";

    private static volatile MainViewController.Facade INSTANCE = null;

    public static MainViewController.Facade instance() {
        return INSTANCE;
    }

    private final ReatmetricPluginInspector serviceInspector = new ReatmetricPluginInspector();

    @FXML
    public Label timeLbl;
    @FXML
    private Accordion sideAccordion;
    @FXML
    private SplitPane mainSplitter;
    @FXML
    private ImageView bannerImage;

    @FXML
    private TabPane viewTabPane;
    @FXML
    private VBox buttonBox;

    @FXML
    private Circle nominalCrl;

    @FXML
    private final PopOver debugPopOver = new PopOver();

    @FXML
    private Label systemLbl;
    @FXML
    private Label statusLbl;
    @FXML
    private ProgressBar globalProgress;

    @FXML
    private Button connectButton;
	private final PopOver connectPopOver = new PopOver();

	private final PopOver infoPopOver = new PopOver();

	@FXML
    private Button detachButton;

	@FXML
    private ModelBrowserViewController modelController;

    private final PopOver messagePopOver = new PopOver();
    private AckMessageDialogController ackMessageController;
    private Timeline alarmFlashTimeline;

    public AbstractDisplayController openPerspective(String viewName) {
        for(Node n : buttonBox.getChildren()) {
            if(n instanceof Button &&
                    n.getProperties().get(VIEW_NAME) != null &&
                    n.getProperties().get(VIEW_NAME).equals(viewName)) {
                // Found
                return activatePerspective(n);
            }
        }
        return null;
    }

    @FXML
    private void viewAction(Event event) {
        activatePerspective((Node) event.getSource());
    }

    private AbstractDisplayController activatePerspective(Node viewButton) {
        if (viewButton.getProperties().get(VIEW_LOCATION) == null) {
            return null;
        }
        // If a tab is already created, select it
        Tab found = null;
        for (Tab n : viewTabPane.getTabs()) {
            if (n.getId() != null && n.getId().equals(viewButton.getProperties().get(VIEW_LOCATION))) {
                found = n;
                break;
            }
        }
        if (found != null) {
            viewTabPane.getSelectionModel().select(found);
            return ((Pair<Node, AbstractDisplayController>) found.getUserData()).getSecond();
        } else {
            try {
                return createView(viewButton);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Cannot load view: " + viewButton.getProperties().get(VIEW_LOCATION) + ": " + e.getMessage(), e);
                return null;
            }
        }
    }

    private AbstractDisplayController createView(Node viewButton) throws IOException {
        String perspectiveId = viewButton.getProperties().get(VIEW_LOCATION).toString();
        URL viewUrl = getClass().getResource(perspectiveId);
        FXMLLoader loader = new FXMLLoader(viewUrl);
        Node view = loader.load();
        final AbstractDisplayController ctrl = loader.getController();

        Tab t = new Tab(getNodeText(viewButton), view);
        Image img = getNodeImg(viewButton);
        if(img != null) {
            t.setGraphic(new ImageView(img));
        }
        t.setId(perspectiveId);
        viewTabPane.getTabs().add(t);
        // Add stop on tab close for any type of subscription in the controller
        t.setOnClosed(event -> {
            if(!ctrl.isDetached()) {
                ctrl.dispose();
            }
        });
        // Set the view and ctrl as user data in the tab
        t.setUserData(Pair.of(view, ctrl));
        // Add detachable menu entry
        registerDetachableTab(t, view, ctrl);
        viewTabPane.getSelectionModel().select(t);

        return ctrl;
    }

    private void registerDetachableTab(Tab t, Node view, AbstractDisplayController ctrl) {
        if(t.getContextMenu() == null) {
            t.setContextMenu(new ContextMenu());
        }
        MenuItem detachMenuItem = new MenuItem("Detach");
        t.getContextMenu().getItems().add(detachMenuItem);
        detachMenuItem.setOnAction(event -> {
            detachTab(t, view, ctrl);
        });
    }

    private void detachTab(Tab t, Node view, AbstractDisplayController ctrl) {
        // Create a detached scene parent
        Stage stage = new Stage();
        t.setContent(null);
        t.setOnCloseRequest(null);
        this.viewTabPane.getTabs().remove(t);
        // this.tab2contents.remove(t); // if removed, there will be no forwards of system status change
        Scene scene = new Scene((Parent) view, view.getLayoutBounds().getWidth(), view.getLayoutBounds().getHeight());
        scene.getStylesheets().add(CSS_URL.toExternalForm());

        stage.setScene(scene);
        stage.setTitle(t.getText());

        Image icon = new Image(ReatmetricUI.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/logos/logo-small-color-32px.png"));
        stage.getIcons().add(icon);
        ctrl.setDetached(stage);
        stage.setOnCloseRequest(ev -> {
            ctrl.dispose();
            stage.close();
        });

        stage.show();
    }

    private Image getNodeImg(Node viewButton) {
        if (viewButton.getProperties().get(VIEW_IMAGE) != null) {
            String imageLocation = viewButton.getProperties().get(VIEW_IMAGE).toString();
            try {
                return new Image(getClass().getResourceAsStream(imageLocation));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    private String getNodeText(Node viewButton) {
        if (viewButton.getProperties().get(VIEW_NAME) != null) {
            return viewButton.getProperties().get(VIEW_NAME).toString();
        } else {
            return "Unknown";
        }
    }

    @FXML
    private void connectAction(ActionEvent event) {
        if (!ReatmetricUI.selectedSystem().isPresent()) {
			showConnectPopOver();
		} else {
			showDisconnectPopOver();
		}
    }

	private void showDisconnectPopOver() {
		VBox layoutBox = new VBox();
		HBox systemBox = new HBox();

		Label text = new Label("Confirm disconnection");
		text.setFont(Font.font("Sans Serif", FontWeight.BOLD, FontPosture.REGULAR, 12));
		layoutBox.getChildren().add(text);
		layoutBox.getChildren().add(systemBox);
		layoutBox.setSpacing(8);
		systemBox.setSpacing(8);
		Button connectToSystemButton = new Button("Disconnect");
		systemBox.getChildren().addAll(connectToSystemButton);
		layoutBox.setPadding(new Insets(8));
		connectPopOver.setArrowLocation(PopOver.ArrowLocation.LEFT_TOP);
		connectPopOver.setContentNode(layoutBox);
		// Set the CSS
		systemBox.getStylesheets().add(getClass().getClassLoader()
				.getResource("eu/dariolucia/reatmetric/ui/fxml/css/MainView.css").toExternalForm());
		// Set the callback
		connectToSystemButton.setOnAction(actionEvent -> {
			connectPopOver.hide();
			ReatmetricUI.threadPool(getClass()).execute(() -> {
				ReatmetricUI.selectedSystem().setSystem(null);
			});
		});
		connectPopOver.show(connectButton);
	}

	private void showConnectPopOver() {
		VBox layoutBox = new VBox();
    	HBox systemBox = new HBox();

    	Label text = new Label("Select system to connect");
		text.setFont(Font.font("Sans Serif", FontWeight.BOLD, FontPosture.REGULAR, 12));
		layoutBox.getChildren().add(text);
		layoutBox.getChildren().add(systemBox);
		layoutBox.setSpacing(8);
		systemBox.setSpacing(8);
		ComboBox<String> systemCombo = new ComboBox<>();
		Button connectToSystemButton = new Button("Connect");
		systemBox.getChildren().addAll(systemCombo, connectToSystemButton);
		// First: let's retrieve the list of available systems and set them in the combo
		List<String> systems = serviceInspector.getAvailableSystems();
		systemCombo.setItems(FXCollections.observableList(systems));
		// Select the first one if any
		if (!systems.isEmpty()) {
			systemCombo.setValue(systems.get(0));
		} else {
			// Disable the login button if there is no system
			connectToSystemButton.setDisable(true);
		}
		layoutBox.setPadding(new Insets(8));
		connectPopOver.setArrowLocation(PopOver.ArrowLocation.LEFT_TOP);
        connectPopOver.setDetachable(false);
		connectPopOver.setContentNode(layoutBox);
		// Set the CSS
		systemBox.getStylesheets().add(getClass().getClassLoader()
				.getResource("eu/dariolucia/reatmetric/ui/fxml/css/MainView.css").toExternalForm());
		// Set the callback
		connectToSystemButton.setOnAction(actionEvent -> {
			connectPopOver.hide();
			// Avoid that the user clicks again on the menu item
			this.connectButton.setDisable(true);
			this.connectButton.setGraphic(new ProgressIndicator());
			final String selectedSystem = systemCombo.getSelectionModel().getSelectedItem();
			ReatmetricUI.threadPool(getClass()).execute(() -> {
				try {
					ReatmetricUI.selectedSystem().setSystem(this.serviceInspector.getSystem(selectedSystem));
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Error when connecting to system " + selectedSystem + ": " + e.getMessage(), e);
					Platform.runLater(() -> {
						this.connectButton.setDisable(false);
						this.connectButton.setGraphic(new ImageView(CONNECT_IMAGE));
					});
				}
			});
		});

		connectPopOver.show(connectButton);
	}

    @FXML
    private void debugAction(Event actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getClassLoader()
                    .getResource("eu/dariolucia/reatmetric/ui/fxml/DebugDialog.fxml"));

            debugPopOver.setContentNode(root);
            debugPopOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_LEFT);
            debugPopOver.setTitle("Debug Information");
            debugPopOver.show(nominalCrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	@FXML
    private void aboutAction(Event actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getClassLoader()
                    .getResource("eu/dariolucia/reatmetric/ui/fxml/AboutDialog.fxml"));

			infoPopOver.setContentNode(root);
			infoPopOver.setArrowLocation(PopOver.ArrowLocation.RIGHT_TOP);
			infoPopOver.setDetachable(false);
			infoPopOver.setTitle("About ReatMetric...");
			infoPopOver.show(bannerImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void systemLabelAction(MouseEvent actionEvent) {
        if(ReatmetricUI.selectedSystem().getSystem() != null) {
            messagePopOver.show(systemLbl);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Set the instance
        INSTANCE = new Facade();

        // Set tabpane image
        viewTabPane.setStyle("-fx-background-image: url(\"/eu/dariolucia/reatmetric/ui/fxml/images/logos/logo-small-color-128px.png\"); -fx-background-repeat: no-repeat; -fx-background-position: center;");

        // Hook width property of view button to vbox width
        for (Node n : buttonBox.getChildren()) {
            if (n instanceof Button) {
                Button viewButton = (Button) n;
                viewButton.prefWidthProperty().bind(buttonBox.widthProperty());
            }
        }

        // Set initial connection image
        this.connectButton.setGraphic(new ImageView(CONNECT_IMAGE));

        // Add the subscriber
        ReatmetricUI.selectedSystem().addSubscriber(this);

        // Register the status label
        ReatmetricUI.registerStatusLabel(this.statusLbl);

        // Expand first
        sideAccordion.setExpandedPane(sideAccordion.getPanes().get(0));

        debugPopOver.setHideOnEscape(true);
        infoPopOver.setHideOnEscape(true);
        messagePopOver.setHideOnEscape(true);
        connectPopOver.setHideOnEscape(true);

        // Create ack table view
        try {
            URL datePickerUrl = getClass().getResource("/eu/dariolucia/reatmetric/ui/fxml/AckMessageDialog.fxml");
            FXMLLoader loader = new FXMLLoader(datePickerUrl);
            Parent root = loader.load();
            ackMessageController = loader.getController();
            messagePopOver.setContentNode(root);
            messagePopOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_LEFT);
            messagePopOver.setTitle("Messages for acknowledgement");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Flashing timeline
        alarmFlashTimeline = new Timeline(
                new KeyFrame(Duration.seconds(0.5), e -> {
                    systemLbl.setStyle(SYSTEM_LABEL_CSS_STYLE_ALARM);
                }),
                new KeyFrame(Duration.seconds(1.0), e -> {
                    systemLbl.setStyle(SYSTEM_LABEL_CSS_STYLE_NOALARM);
                })
        );
        alarmFlashTimeline.setCycleCount(Animation.INDEFINITE);

        // digital clock, update 1 per second.
        final Timeline digitalTime = new Timeline(
            new KeyFrame(Duration.seconds(0),
                    actionEvent -> timeLbl.setText(InstantCellFactory.DATE_TIME_FORMATTER_SECONDS.format(Instant.now()))
            ),
            new KeyFrame(Duration.seconds(1))
        );
        digitalTime.setCycleCount(Animation.INDEFINITE);
        digitalTime.play();

        // detach button visible/invisible depending on tab selection
        detachButton.setVisible(false);
        viewTabPane.getSelectionModel().selectedItemProperty().addListener((tabPane) -> detachButton.setVisible(!viewTabPane.getSelectionModel().isEmpty()));
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
            registerAcknowledgeMonitor();
            try {
                this.systemLbl.setText(system.getName());
                ReatmetricUI.setStatusLabel("System " + system.getName() + " connected");
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Error on system connected: " + e.getMessage(), e);
            }
        });
    }

    private void registerAcknowledgeMonitor() {
        ackMessageController.activate(this::signalAckStatusChanged);
    }

    @Override
    public void systemDisconnected(IReatmetricSystem system) {
        Platform.runLater(() -> {
            disableMainViewItems();
            deregisterAcknowledgeMonitor();
            try {
                ReatmetricUI.setStatusLabel("System " + system.getName() + " disconnected");
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Error on system connected: " + e.getMessage(), e);
            }
        });
    }

    private void deregisterAcknowledgeMonitor() {
        ackMessageController.deactivate();
    }

    @Override
    public void systemStatusUpdate(SystemStatus status) {
        Platform.runLater(() -> {
            updateStatusIndicator(status);
        });
    }

    protected void signalAckStatusChanged(boolean inAlarm) {
        if(inAlarm) {
            if(alarmFlashTimeline.getStatus() != Animation.Status.RUNNING) {
                alarmFlashTimeline.play();
            }
        } else {
            alarmFlashTimeline.pause();
            systemLbl.setStyle(SYSTEM_LABEL_CSS_STYLE_NOALARM);
        }
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

        // Disable the view buttons
        buttonBox.getChildren().forEach(o -> o.setDisable(true));

        // Change button picture
        connectButton.setDisable(false);
        connectButton.setGraphic(new ImageView(CONNECT_IMAGE));

        // Disable the system label
        this.systemLbl.setText("---");
        this.systemLbl.setDisable(true);
    }

    private void enableMainViewItems() {
        // Enable the view buttons
        buttonBox.getChildren().forEach(o -> o.setDisable(false));

        // Change button picture
        connectButton.setDisable(false);
        connectButton.setGraphic(new ImageView(DISCONNECT_IMAGE));

        // Enable the system label
        this.systemLbl.setDisable(false);
    }

    private void updateStatusIndicator(SystemStatus state) {
        Platform.runLater(() -> {
            switch (state) {
                case ALARM:
                    this.nominalCrl.setFill(Paint.valueOf(ALARM_COLOR));
                    break;
                case WARNING:
                    this.nominalCrl.setFill(Paint.valueOf(WARNING_COLOR));
                    break;
                case NOMINAL:
                    this.nominalCrl.setFill(Paint.valueOf(NOMINAL_COLOR));
                    break;
                default:
                    this.nominalCrl.setFill(Paint.valueOf(DISABLED_COLOR));
                    break;
            }
        });
    }

    @FXML
    private void detachMouseClicked(ActionEvent e) {
        Tab toDetach = viewTabPane.getSelectionModel().getSelectedItem();
        if(toDetach != null) {
            Pair<Node, AbstractDisplayController> data = (Pair<Node, AbstractDisplayController>) toDetach.getUserData();
            detachTab(toDetach, data.getFirst(), data.getSecond());
        }
    }

    public class Facade {

        public AbstractDisplayController openPerspective(String name) {
            return MainViewController.this.openPerspective(name);
        }

        public ModelBrowserViewController getModelController() {
            return modelController;
        }
    }
}
