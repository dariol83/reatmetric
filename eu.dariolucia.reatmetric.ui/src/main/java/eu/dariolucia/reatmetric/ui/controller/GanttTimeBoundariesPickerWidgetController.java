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

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class GanttTimeBoundariesPickerWidgetController implements Initializable {

    @FXML
    private Spinner<Integer> pastHourSpn;
    @FXML
    private Spinner<Integer> pastMinuteSpn;
    @FXML
    private Spinner<Integer> pastSecondSpn;

    @FXML
    private Spinner<Integer> aheadHourSpn;
    @FXML
    private Spinner<Integer> aheadMinuteSpn;
    @FXML
    private Spinner<Integer> aheadSecondSpn;

    @FXML
    private Spinner<Integer> retrievalHourSpn;
    @FXML
    private Spinner<Integer> retrievalMinuteSpn;

    private Runnable actionAfterSelection;

    private int pastDuration;

    private int futureDuration;

    private int retrievalDuration;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.pastHourSpn.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 120, 0));
        this.pastMinuteSpn.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        this.pastSecondSpn.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        this.aheadHourSpn.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 120, 0));
        this.aheadMinuteSpn.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        this.aheadSecondSpn.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        this.retrievalHourSpn.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 120, 0));
        this.retrievalMinuteSpn.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
    }

    @FXML
    private void selectButtonPressed(ActionEvent e) {
        this.pastDuration = deriveFromWidgets(pastHourSpn.getValue(), pastMinuteSpn.getValue(), pastSecondSpn.getValue());
        this.futureDuration = deriveFromWidgets(aheadHourSpn.getValue(), aheadMinuteSpn.getValue(), aheadSecondSpn.getValue());
        this.retrievalDuration = retrievalHourSpn.getValue() * 3600 + retrievalMinuteSpn.getValue() * 60;
        if (this.actionAfterSelection != null) {
            this.actionAfterSelection.run();
        }
    }

    private int deriveFromWidgets(Integer h, Integer m, Integer s) {
        return h * 3600 + m * 60 + s;
    }

    public void setActionAfterSelection(Runnable r) {
        this.actionAfterSelection = r;
    }

    public void setLive(boolean live) {
        this.pastHourSpn.setDisable(!live);
        this.pastMinuteSpn.setDisable(!live);
        this.pastSecondSpn.setDisable(!live);
        this.aheadHourSpn.setDisable(!live);
        this.aheadMinuteSpn.setDisable(!live);
        this.aheadSecondSpn.setDisable(!live);
        this.retrievalHourSpn.setDisable(live);
        this.retrievalMinuteSpn.setDisable(live);
    }

    public void setInterval(int pastDuration, int futureDuration, int retrievalDuration) {
        this.pastDuration = pastDuration;
        this.futureDuration = futureDuration;
        this.retrievalDuration = retrievalDuration;
        updateWidgets();
    }

    public int getPastDuration() {
        return pastDuration;
    }

    public int getFutureDuration() {
        return futureDuration;
    }

    public int getRetrievalDuration() {
        return retrievalDuration;
    }

    private void updateWidgets() {
        int pastH = pastDuration / 3600;
        int pastM = (pastDuration % 3600) / 60;
        int pastS = pastDuration % 60;
        this.pastHourSpn.getValueFactory().setValue(pastH);
        this.pastMinuteSpn.getValueFactory().setValue(pastM);
        this.pastSecondSpn.getValueFactory().setValue(pastS);

        int aheadH = futureDuration / 3600;
        int aheadM = (futureDuration % 3600) / 60;
        int aheadS = futureDuration % 60;
        this.aheadHourSpn.getValueFactory().setValue(aheadH);
        this.aheadMinuteSpn.getValueFactory().setValue(aheadM);
        this.aheadSecondSpn.getValueFactory().setValue(aheadS);

        this.retrievalHourSpn.getValueFactory().setValue(retrievalDuration/3600);
        this.retrievalMinuteSpn.getValueFactory().setValue((retrievalDuration%3600)/60);
    }
}
