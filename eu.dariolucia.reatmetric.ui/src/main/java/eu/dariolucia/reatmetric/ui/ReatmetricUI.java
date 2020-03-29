/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.ui.plugin.ReatmetricServiceHolder;
import eu.dariolucia.reatmetric.ui.preferences.PreferencesManager;
import eu.dariolucia.reatmetric.ui.utils.DialogUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 *
 * @author dario
 */
public class ReatmetricUI extends Application {
    
    public static final String APPLICATION_NAME = "Reatmetric UI";
    
    public static final String APPLICATION_VERSION = "0.1.0";
    
    private static final ReatmetricServiceHolder SELECTED_SYSTEM = new ReatmetricServiceHolder();
    
    public static ReatmetricServiceHolder selectedSystem() {
        return SELECTED_SYSTEM;
    }
    
    private static final Map<Class<?>, ExecutorService> THREAD_POOL = new ConcurrentHashMap<>();
    
    public static ExecutorService threadPool(final Class<?> clazz) {
    	ExecutorService toReturn = null;
    	synchronized (THREAD_POOL) {
			toReturn = THREAD_POOL.get(clazz);
			if(toReturn == null) {
				toReturn = Executors.newFixedThreadPool(1, r -> {
                    Thread t = new Thread(r);
                    t.setName(clazz.getSimpleName() + " Worker Thread");
                    t.setDaemon(true);
                    return t;
                });
				THREAD_POOL.put(clazz, toReturn);
			}
		}
        return toReturn;
    }

    private static void shutdownThreadPool() {
        synchronized (THREAD_POOL) {
            for(Map.Entry<Class<?>, ExecutorService> entry : THREAD_POOL.entrySet()) {
                entry.getValue().shutdownNow();
            }
        }
    }
    
    private static final PreferencesManager PREFERENCES = new PreferencesManager();
    
    public static PreferencesManager preferences() {
        return PREFERENCES;
    }
    
    private static Label STATUS_LABEL = null;

	private static Consumer<AlarmState> STATUS_INDICATOR = null;
    
    public static void registerStatusLabel(Label l) {
        STATUS_LABEL = l;
    }
    
    public static void setStatusLabel(String s) {
        Platform.runLater(() -> {
            if(STATUS_LABEL != null) {
                STATUS_LABEL.setText(s);
            }
        });
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(ReatmetricUI.class.getResource("/eu/dariolucia/reatmetric/ui/fxml/MainView.fxml"));
        
        Scene scene = new Scene(root);
        // scene.getStylesheets().add(getClass().getResource("/eu/dariolucia/reatmetric/ui/css/MainView.css").toExternalForm());
        
        stage.setScene(scene);
        stage.setTitle("Reatmetric UI");

        Image icon = new Image(ReatmetricUI.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/logo_icon.png"));
        stage.getIcons().add(icon);

        stage.setOnCloseRequest(event -> {
            event.consume();
            shutdown();
        });
        
        stage.show();
    }

    @Override
    public void stop() {
        ReatmetricUI.threadPool(ReatmetricUI.class).shutdown();
    }

    public static void shutdown() {
        if (DialogUtils.confirm("Exit Reatmetric UI", "Exit Reatmetric UI", "Do you want to close Reatmetric UI?")) {
            ReatmetricUI.shutdownThreadPool();
            ReatmetricUI.selectedSystem().setSystem(null);
            Platform.exit();
            System.exit(0);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
