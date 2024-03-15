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


package eu.dariolucia.reatmetric.ui;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.ui.plugin.ReatmetricServiceHolder;
import eu.dariolucia.reatmetric.ui.preferences.PreferencesManager;
import eu.dariolucia.reatmetric.ui.utils.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dario
 */
public class ReatmetricUI extends Application {

    private static final Logger LOG = Logger.getLogger(ReatmetricUI.class.getName());

    public static final String APPLICATION_NAME = "ReatMetric UI";
    
    public static final String APPLICATION_VERSION = "0.1.0";

    private static final String REATMETRIC_USERNAME_KEY = "reatmetric.username";

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
                entry.getValue().shutdown();
            }
            for(Map.Entry<Class<?>, ExecutorService> entry : THREAD_POOL.entrySet()) {
                try {
                    boolean terminated = entry.getValue().awaitTermination(1, TimeUnit.SECONDS);
                    LOG.log(Level.FINE, "ThreadPool for class " + entry.getKey().getSimpleName() + " " + (terminated ? "terminated" : "NOT terminated"));
                } catch (InterruptedException e) {
                    // Ignore
                }
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
        FxUtils.runLater(() -> {
            if(STATUS_LABEL != null) {
                STATUS_LABEL.setText(s);
            }
        });
    }

    public static String username() {
        String definedUserName = System.getProperty(REATMETRIC_USERNAME_KEY);
        if(definedUserName == null) {
            definedUserName = System.getProperty("user.name");
        }
        return definedUserName;
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(ReatmetricUI.class.getResource("/eu/dariolucia/reatmetric/ui/fxml/MainView.fxml"));
        CssHandler.applyTo(root);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle(APPLICATION_NAME);
        stage.setMaximized(true);
        Image icon = new Image(ReatmetricUI.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/logos/logo-small-color-32px.png"));
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
        if (DialogUtils.confirm("Exit " + APPLICATION_NAME, "Exit " + APPLICATION_NAME, "Do you want to close " + APPLICATION_NAME + "?")) {
            LOG.info("Reatmetric UI shutdown sequence");
            LOG.info("Shutting down display coordinators");
            UserDisplayCoordinator.instance().dispose();
            ParameterDisplayCoordinator.instance().dispose();
            MimicsDisplayCoordinator.instance().dispose();
            LOG.info("Disconnection from system");
            ReatmetricUI.selectedSystem().setSystem(null);
            LOG.info("System disconnection done");
            // Wait for completion
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LOG.info("Shutting down UI thread pool");
            ReatmetricUI.shutdownThreadPool();
            LOG.info("UI thread pool shut down, exiting JFX platform");
            Platform.exit();
            LOG.info("Freeing up memory");
            System.gc();
            LOG.info("Reatmetric UI shutdown sequence completed. Have a nice day.");
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
