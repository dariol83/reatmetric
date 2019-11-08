/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
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
