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
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.net.URL;
import java.util.*;

public class DebugDialogController implements Initializable {


    @FXML
    public VBox vboxParent;

    private final Timer sampler = new Timer();

    private final Map<Pair<String, String>, Control> id2control = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        vboxParent.setPadding(new Insets(4));
        // Subscribe to visibility
        vboxParent.visibleProperty().addListener(observable -> {
            if(!vboxParent.isVisible()) {
                id2control.clear();
                sampler.cancel();
            }
        });
        // Start sampler
        sampler.schedule(new TimerTask() {
            @Override
            public void run() {
                sample();
            }
        }, 1000, 2000);
    }

    private void sample() {
        IReatmetricSystem system = ReatmetricUI.selectedSystem().getSystem();
        if(system != null) {
            List<DebugInformation> info = system.currentDebugInfo();
            if(info == null || info.isEmpty()) {
                setEmptyDebugInformation();
            } else {
                Platform.runLater(() -> {
                    updateData(info);
                });
            }
        } else {
            setEmptyDebugInformation();
        }
    }

    private void setEmptyDebugInformation() {
        Platform.runLater(() -> {
            vboxParent.getChildren().clear();
            id2control.clear();
            Label l = new Label("No debug information currently available");
            l.setPadding(new Insets(4.0));
            vboxParent.getChildren().add(l);
        });
    }

    private void updateData(List<DebugInformation> info) {
        if(id2control.isEmpty()) {
            initControls(info);
        } else {
            updateControls(info);
        }
    }

    private void updateControls(List<DebugInformation> info) {
        for(DebugInformation di : info) {
            Pair<String, String> key = Pair.of(di.getElement(), di.getName());
            Control ctr = id2control.get(key);
            if(ctr != null) {
                if(ctr instanceof ProgressBar) {
                    ProgressBar bar = (ProgressBar) ctr;
                    bar.setProgress(((Number) di.getMeasure()).doubleValue() / ((Number) di.getMaximum()).doubleValue());
                    bar.setTooltip(new Tooltip(di.getMeasure() + " / " + di.getMaximum()));
                } else {
                    ((Label)ctr).setText(Objects.toString(di.getMeasure(), ""));
                }
            }
        }
    }

    private void initControls(List<DebugInformation> info) {
        String previousElement = null;
        for(DebugInformation di : info) {
            if(!Objects.equals(previousElement, di.getElement())) {
                addSeparator(di.getElement());
            }
            addControl(di);
            previousElement = di.getElement();
        }
    }

    private void addControl(DebugInformation di) {
        Pair<String, String> key = Pair.of(di.getElement(), di.getName());
        Control ctr = null;
        HBox inner = new HBox();
        inner.setSpacing(4);
        Label nameLbl = new Label(di.getName());
        nameLbl.setPrefWidth(150);
        nameLbl.setPrefHeight(16);
        inner.getChildren().add(nameLbl);
        // if the measure is a number and there is a maximum, then use progress bar
        if(di.getMeasure() instanceof Number && di.getMaximum() != null) {
            ProgressBar bar = new ProgressBar();
            bar.setPrefHeight(16);
            bar.setPrefWidth(100);
            bar.setProgress(((Number) di.getMeasure()).doubleValue() / ((Number) di.getMaximum()).doubleValue());
            bar.setTooltip(new Tooltip(di.getMeasure() + " / " + di.getMaximum()));
            ctr = bar;
        } else {
            // else use label
            Label label = new Label(Objects.toString(di.getMeasure(), ""));
            label.setPrefHeight(16);
            label.setPrefWidth(100);
            label.setAlignment(Pos.CENTER_RIGHT);
            label.setTextAlignment(TextAlignment.RIGHT);
            ctr = label;
        }
        inner.getChildren().add(ctr);
        if(di.getUnit() != null) {
            Label unitLabel = new Label(di.getUnit());
            unitLabel.setPrefWidth(120);
            unitLabel.setPrefHeight(16);
            inner.getChildren().add(unitLabel);
        }
        vboxParent.getChildren().add(inner);
        id2control.put(key, ctr);
        vboxParent.layout();
    }

    private void addSeparator(String element) {
        Label l = new Label(element);
        l.setPrefHeight(16);
        l.setStyle("-fx-font-weight: BOLD");
        l.setPadding(new Insets(2));
        vboxParent.getChildren().add(l);
    }
}
