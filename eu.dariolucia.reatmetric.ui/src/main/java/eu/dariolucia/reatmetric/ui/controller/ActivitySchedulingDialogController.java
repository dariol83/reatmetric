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

import eu.dariolucia.reatmetric.api.scheduler.AbsoluteTimeSchedulingTrigger;
import eu.dariolucia.reatmetric.api.scheduler.EventBasedSchedulingTrigger;
import eu.dariolucia.reatmetric.api.scheduler.RelativeTimeSchedulingTrigger;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class ActivitySchedulingDialogController implements Initializable {

    @FXML
    public TextField sourceText;
    @FXML
    public TextField resourcesText;
    @FXML
    public RadioButton absoluteTimeRadio;
    @FXML
    public ToggleGroup triggerToggle;
    @FXML
    public RadioButton relativeTimeRadio;
    @FXML
    public TextField relativeTimeText;
    @FXML
    public TextField externalIdText;
    @FXML
    public RadioButton eventDrivenRadio;
    @FXML
    public TextField eventPathText;
    @FXML
    public TextField protectionTimeText;
    @FXML
    public DatePicker absoluteDatePicker;
    @FXML
    public TextField absoluteTimeText;

    private final SimpleBooleanProperty entriesValid = new SimpleBooleanProperty(false);

    // TODO: add support for latest invocation time, external id specification, conflict strategy, expected duration

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        relativeTimeText.disableProperty().bind(relativeTimeRadio.selectedProperty().not());
        externalIdText.disableProperty().bind(relativeTimeRadio.selectedProperty().not());
        eventPathText.disableProperty().bind(eventDrivenRadio.selectedProperty().not());
        protectionTimeText.disableProperty().bind(eventDrivenRadio.selectedProperty().not());
        absoluteDatePicker.disableProperty().bind(absoluteTimeRadio.selectedProperty().not());
        absoluteTimeText.disableProperty().bind(absoluteTimeRadio.selectedProperty().not());

        sourceText.textProperty().addListener(o -> validate());
        resourcesText.textProperty().addListener(o -> validate());
        ChangeListener<Boolean> changeListener = (o, oldVal, newVal) -> {
            if (newVal) {
                validate();
            }
        };
        absoluteTimeRadio.selectedProperty().addListener(changeListener);
        relativeTimeRadio.selectedProperty().addListener(changeListener);
        eventDrivenRadio.selectedProperty().addListener(changeListener);
        absoluteDatePicker.valueProperty().addListener(o -> validate());
        externalIdText.textProperty().addListener(o -> validate());
        relativeTimeText.textProperty().addListener(o -> validate());
        eventPathText.textProperty().addListener(o -> validate());
        protectionTimeText.textProperty().addListener(o -> validate());
        absoluteTimeText.textProperty().addListener(o -> validate());

    }

    public void setRequest(SchedulingRequest request) {
        resourcesText.setText(formatResources(request.getResources()));
        sourceText.setText(request.getSource());
        if(request.getTrigger() instanceof AbsoluteTimeSchedulingTrigger) {
            AbsoluteTimeSchedulingTrigger tr = (AbsoluteTimeSchedulingTrigger) request.getTrigger();
            absoluteDatePicker.setValue(LocalDate.ofInstant(tr.getReleaseTime(), ZoneId.of("UTC")));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                    .withZone(ZoneId.of("UTC"));
            absoluteTimeText.setText(formatter.format(tr.getReleaseTime()));
        } else if(request.getTrigger() instanceof RelativeTimeSchedulingTrigger) {
            RelativeTimeSchedulingTrigger tr = (RelativeTimeSchedulingTrigger) request.getTrigger();
            // TODO: support multiple predecessors in UI
        } else if(request.getTrigger() instanceof EventBasedSchedulingTrigger) {
            eventPathText.setText(findEvent(((EventBasedSchedulingTrigger) request.getTrigger()).getEvent()));
            protectionTimeText.setText(String.valueOf (((EventBasedSchedulingTrigger) request.getTrigger()).getProtectionTime() / 1000));
        }
    }

    private String findEvent(int eventId) {
        // TODO: support event lookup int -> string
        return null;
    }

    private String formatResources(Set<String> resources) {
        StringBuilder sb = new StringBuilder("");
        for(String s : resources) {
            sb.append(s).append(",");
        }
        if(sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private void validate() {
        boolean errorFound = false;
        if (sourceText.getText().isBlank()) {
            setError(sourceText, true);
            errorFound = true;
        } else {
            setError(sourceText, false);
        }
        if (resourcesText.getText().isBlank() || getResources() == null) {
            setError(resourcesText, true);
            errorFound = true;
        } else {
            setError(resourcesText, false);
        }
        if(absoluteTimeRadio.isSelected()) {
            if(absoluteDatePicker.getValue() == null) {
                setError(absoluteDatePicker, true);
                errorFound = true;
            } else {
                setError(absoluteDatePicker, false);
            }
            if (absoluteTimeText.getText().isBlank() || getAbsoluteTimeText() == null) {
                setError(absoluteTimeText, true);
                errorFound = true;
            } else {
                setError(absoluteTimeText, false);
            }
        } else {
            setError(absoluteDatePicker, false);
            setError(absoluteTimeText, false);
        }
        if(relativeTimeRadio.isSelected()) {
            if (relativeTimeText.getText().isBlank() || getRelativeTimeText() == null) {
                setError(relativeTimeText, true);
                errorFound = true;
            } else {
                setError(relativeTimeText, false);
            }
            if (externalIdText.getText().isBlank() || getExternalIdText() == null) {
                setError(externalIdText, true);
                errorFound = true;
            } else {
                setError(externalIdText, false);
            }
        } else {
            setError(relativeTimeText, false);
            setError(externalIdText, false);
        }
        if(eventDrivenRadio.isSelected()) {
            if (eventPathText.getText().isBlank()) {
                setError(eventPathText, true);
                errorFound = true;
            } else {
                setError(eventPathText, false);
            }
            if (protectionTimeText.getText().isBlank() || getProtectionTimeText() == null) {
                setError(protectionTimeText, true);
                errorFound = true;
            } else {
                setError(protectionTimeText, false);
            }
        } else {
            setError(eventPathText, false);
            setError(protectionTimeText, false);
        }

        entriesValid.set(!errorFound);
    }

    public Integer getProtectionTimeText() {
        try {
            return Integer.parseInt(protectionTimeText.getText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Long getExternalIdText() {
        try {
            return Long.parseLong(externalIdText.getText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Integer getRelativeTimeText() {
        try {
            return Integer.parseInt(relativeTimeText.getText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public LocalTime getAbsoluteTimeText() {
        try {
            return LocalTime.parse(absoluteTimeText.getText());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public Set<String> getResources() {
        String[] spl = resourcesText.getText().split(",", -1);
        String[] toReturn = new String[spl.length];
        for(int i = 0; i < spl.length; ++i) {
            toReturn[i] = spl[i].trim();
            if(toReturn[i].indexOf(' ') != -1 || toReturn[i].isBlank()) { // Spaces are not allowed, empty string resources are not allowed
                return null;
            }
        }
        return new LinkedHashSet<>(Arrays.asList(toReturn));
    }

    private void setError(Node node, boolean error) {
        if(error) {
            node.setStyle("-fx-background-color: red;");
        } else {
            node.setStyle("");
        }
    }

    public SimpleBooleanProperty entriesValidProperty() {
        return entriesValid;
    }
}
