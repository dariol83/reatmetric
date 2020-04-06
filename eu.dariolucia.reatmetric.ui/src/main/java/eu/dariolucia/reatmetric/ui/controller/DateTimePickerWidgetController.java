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

import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class DateTimePickerWidgetController implements Initializable {

    @FXML
    private Spinner<Integer> hourSpn;
    @FXML
    private Spinner<Integer> minuteSpn;
    @FXML
    private Spinner<Integer> secondSpn;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Button selectBtn;
    
    private Runnable actionAfterSelection;
    
    // The result of the selection
    private Instant selectedTime = null;
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.hourSpn.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        this.minuteSpn.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        this.secondSpn.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        this.datePicker.setValue(LocalDate.now());
    }  
    
    @FXML
    private void selectButtonPressed(ActionEvent e) {
        this.selectedTime = deriveFromWidgets();
        if(this.actionAfterSelection != null) {
            this.actionAfterSelection.run();
        }
    }
  
    public void setActionAfterSelection(Runnable r) {
        this.actionAfterSelection = r;
    }
    
    public void setSelectedTime(Instant t) {
        this.selectedTime = t;
        updateWidgets();
    }

    public Instant getSelectedTime() {
        return this.selectedTime;
    }
    
    private void updateWidgets() {
        LocalDateTime ldt = LocalDateTime.ofInstant(this.selectedTime, ZoneOffset.UTC);
        this.hourSpn.getValueFactory().setValue(ldt.getHour());
        this.minuteSpn.getValueFactory().setValue(ldt.getMinute());
        this.secondSpn.getValueFactory().setValue(ldt.getSecond());
        this.datePicker.setValue(ldt.toLocalDate());
    }

    private Instant deriveFromWidgets() {
        LocalDateTime ldt = LocalDateTime.of(this.datePicker.getValue(), LocalTime.of((int) this.hourSpn.getValue(),(int) this.minuteSpn.getValue(),(int) this.secondSpn.getValue()));
        return ldt.toInstant(ZoneOffset.UTC);
    }
}
