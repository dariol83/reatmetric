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

import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.mimics.MimicsEngine;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.w3c.dom.Document;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;

public class MimicsSvgViewController implements Initializable {

    private static final Image ZOOM_IMG = new Image(ConnectorStatusWidgetController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/16px/picture.svg.png"));

    @FXML
    protected ScrollPane scrollPane;
    @FXML
    protected WebView webView;
    @FXML
    protected ImageView zoomImage;
    @FXML
    protected Slider zoomSlider;

    private boolean loaded = false;

    private MimicsEngine mimicsEngine;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        webView.setDisable(true);
        zoomSlider.setDisable(true);
        webView.setContextMenuEnabled(false);
        zoomImage.setImage(ZOOM_IMG);
        zoomSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(loaded) {
                webView.setZoom(zoomSlider.getValue() / 100.0);
            }
        });
    }

    public void configure(File svgFile, final Consumer<Set<String>> notifier) {
        zoomSlider.setValue(100);
        webView.setDisable(false);
        zoomSlider.setDisable(false);
        WebEngine engine = webView.getEngine();
        String url = svgFile.toURI().toString();
        engine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Get the DOM and process the elements (in the next run of the UI thread)
                ReatmetricUI.threadPool(MimicsSvgViewController.class).execute(
                        () -> prepareMimics(notifier)
                );
            }
        });
        engine.load(url);
        loaded = true;
    }

    private void prepareMimics(final Consumer<Set<String>> notifier) {
        Document svgDom = webView.getEngine().getDocument();
        final Set<String> parameters = prepareMimicsEngine(svgDom); // Time consuming operation, defer it to working thread
        Platform.runLater(() -> {
            notifier.accept(parameters);
        });
    }

    private Set<String> prepareMimicsEngine(Document svgDom) {
        mimicsEngine = new MimicsEngine(svgDom);
        mimicsEngine.initialise();
        return mimicsEngine.getParameters();
    }

    public void refresh(Map<SystemEntityPath, ParameterData> parameters, Set<SystemEntityPath> updatedItems) {
        if(mimicsEngine != null) {
            mimicsEngine.refresh(parameters, updatedItems);
        }
    }
}
