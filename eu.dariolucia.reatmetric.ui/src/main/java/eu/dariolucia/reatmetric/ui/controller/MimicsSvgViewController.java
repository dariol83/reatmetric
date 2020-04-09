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

import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.mimics.MimicsEngine;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.w3c.dom.Document;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;

public class MimicsSvgViewController implements Initializable {

    private static final Image FIT_TO_AREA_IMG = new Image(MimicsSvgViewController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/16px/picture.svg.png"));
    private static final Image REAL_SIZE_IMG = new Image(MimicsSvgViewController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/16px/qr-code.svg.png"));
    private static final Image MINUS_ZOOM_IMG = new Image(MimicsSvgViewController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/16px/minus-circle.svg.png"));
    private static final Image PLUS_ZOOM_IMG = new Image(MimicsSvgViewController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/16px/plus-circle.svg.png"));

    @FXML
    public ImageView realSizeImage;
    @FXML
    public ImageView fitToAreaImage;
    @FXML
    public ImageView minusZoomImage;
    @FXML
    public ImageView plusZoomImage;

    @FXML
    protected ScrollPane scrollPane;
    @FXML
    protected WebView webView;
    @FXML
    protected Slider zoomSlider;

    private boolean loaded = false;

    private MimicsEngine mimicsEngine;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        webView.setDisable(true);
        zoomSlider.setDisable(true);
        webView.setContextMenuEnabled(false);

        fitToAreaImage.setImage(FIT_TO_AREA_IMG);
        realSizeImage.setImage(REAL_SIZE_IMG);
        minusZoomImage.setImage(MINUS_ZOOM_IMG);
        plusZoomImage.setImage(PLUS_ZOOM_IMG);

        zoomSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(loaded) {
                webView.setZoom(zoomSlider.getValue() / 100.0);
            }
        });

        // TODO: remove
        webView.getEngine().documentProperty().addListener((prop, oldDoc, newDoc) -> {
            String heightText = webView.getEngine().executeScript(
                    "window.getComputedStyle(document.svg, null).getPropertyValue('height')"
            ).toString();

            System.out.println("heighttext: " + heightText);
        });
    }

    @FXML
    public void realSizeClick(MouseEvent mouseEvent) {
        zoomSlider.setValue(100);
    }

    @FXML
    public void fitToAreaClick(MouseEvent mouseEvent) {
        // Real size
        double currHeight = webView.getHeight() / webView.getZoom();
        Object jsHeight = webView.getEngine().executeScript("document.height");
        double currWidth = webView.getWidth() / webView.getZoom();
        // Viewport size
        double vHeight = scrollPane.getViewportBounds().getHeight();
        double vWidth = scrollPane.getViewportBounds().getWidth();
        // Compute the h and w ratio
        double hRatio = vHeight / currHeight;
        double wRatio = vWidth / currWidth;
        // which ratio to use? the smaller?
        zoomSlider.setValue(100 * Math.min(hRatio, wRatio));
    }

    @FXML
    public void minusZoomClick(MouseEvent mouseEvent) {
        double value = zoomSlider.getValue();
        value -= zoomSlider.getMinorTickCount();
        if(value <= 0) {
            value = 0;
        }
        zoomSlider.setValue(value);
    }

    @FXML
    public void plusZoomClick(MouseEvent mouseEvent) {
        double value = zoomSlider.getValue();
        value += zoomSlider.getMinorTickCount();
        if(value >= zoomSlider.getMax()) {
            value = zoomSlider.getMax();
        }
        zoomSlider.setValue(value);
    }

    public void configure(File svgFile, final Consumer<Set<String>> notifier) {
        zoomSlider.setValue(100);
        webView.setDisable(false);
        zoomSlider.setDisable(false);
        WebEngine engine = webView.getEngine();
        String url = svgFile.toURI().toString();
        engine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                prepareMimics(notifier);
            }
        });
        engine.load(url);
        loaded = true;
    }

    private void prepareMimics(final Consumer<Set<String>> notifier) {
        // Time consuming operation, defer it to working thread, but beware, creation/setting of DOM nodes must be done in
        // the UI thread
        ReatmetricUI.threadPool(MimicsSvgViewController.class).execute(() -> {
            Document svgDom = webView.getEngine().getDocument();
            final Set<String> parameters = prepareMimicsEngine(svgDom);
            notifier.accept(parameters);
        });
    }

    private Set<String> prepareMimicsEngine(Document svgDom) {
        mimicsEngine = new MimicsEngine(svgDom);
        mimicsEngine.initialise();
        return mimicsEngine.getParameters();
    }

    public void refresh(List<ParameterData> updatedItems) {
        if(mimicsEngine != null) {
            mimicsEngine.refresh(updatedItems);
        }
    }

}
