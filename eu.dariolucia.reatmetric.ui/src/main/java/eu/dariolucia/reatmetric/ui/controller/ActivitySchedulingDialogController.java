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
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityResolver;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.util.Callback;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class ActivitySchedulingDialogController implements Initializable {

    @FXML
    public TextField sourceText;
    @FXML
    public TextField resourcesText;
    @FXML
    public RadioButton absoluteTimeRadio;
    @FXML
    public RadioButton nowTimeRadio;
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
    public CheckBox enabledCheckBox;
    @FXML
    public DatePicker absoluteDatePicker;
    @FXML
    public TextField absoluteTimeText;

    @FXML
    public TextField expectedDurationText;
    @FXML
    public TextField taskExternalIdText;
    @FXML
    public CheckBox latestExecutionCheckbox;
    @FXML
    public DatePicker latestExecutionDatePicker;
    @FXML
    public TextField latestExecutionTimeText;
    @FXML
    public ChoiceBox<ConflictStrategy> conflictChoice;
    @FXML
    public ChoiceBox<CreationConflictStrategy> creationChoice;

    private final SimpleBooleanProperty entriesValid = new SimpleBooleanProperty(false);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        conflictChoice.getItems().addAll(ConflictStrategy.values());
        conflictChoice.getSelectionModel().select(ConflictStrategy.WAIT.ordinal()); // Select WAIT

        creationChoice.getItems().addAll(CreationConflictStrategy.values());
        creationChoice.getSelectionModel().select(CreationConflictStrategy.ADD_ANYWAY.ordinal()); // Select WAIT

        relativeTimeText.disableProperty().bind(relativeTimeRadio.selectedProperty().not());
        externalIdText.disableProperty().bind(relativeTimeRadio.selectedProperty().not());
        eventPathText.disableProperty().bind(eventDrivenRadio.selectedProperty().not());
        protectionTimeText.disableProperty().bind(eventDrivenRadio.selectedProperty().not());
        enabledCheckBox.disableProperty().bind(eventDrivenRadio.selectedProperty().not());
        absoluteDatePicker.disableProperty().bind(absoluteTimeRadio.selectedProperty().not());
        absoluteTimeText.disableProperty().bind(absoluteTimeRadio.selectedProperty().not());

        latestExecutionDatePicker.disableProperty().bind(latestExecutionCheckbox.selectedProperty().not());
        latestExecutionTimeText.disableProperty().bind(latestExecutionCheckbox.selectedProperty().not());

        sourceText.textProperty().addListener(o -> validate());
        resourcesText.textProperty().addListener(o -> validate());
        ChangeListener<Boolean> changeListener = (o, oldVal, newVal) -> {
            if (newVal) {
                validate();
            }
        };
        nowTimeRadio.selectedProperty().addListener(changeListener);
        absoluteTimeRadio.selectedProperty().addListener(changeListener);
        relativeTimeRadio.selectedProperty().addListener(changeListener);
        eventDrivenRadio.selectedProperty().addListener(changeListener);
        absoluteDatePicker.valueProperty().addListener(o -> validate());
        externalIdText.textProperty().addListener(o -> validate());
        relativeTimeText.textProperty().addListener(o -> validate());
        eventPathText.textProperty().addListener(o -> validate());
        protectionTimeText.textProperty().addListener(o -> validate());
        enabledCheckBox.selectedProperty().addListener(o -> validate());
        absoluteTimeText.textProperty().addListener(o -> validate());
        expectedDurationText.textProperty().addListener(o -> validate());
        taskExternalIdText.textProperty().addListener(o -> validate());
        latestExecutionCheckbox.selectedProperty().addListener(o -> validate());
        latestExecutionDatePicker.valueProperty().addListener(o -> validate());
        latestExecutionTimeText.textProperty().addListener(o -> validate());

        TextFields.bindAutoCompletion(eventPathText, new Callback<AutoCompletionBinding.ISuggestionRequest, Collection<String>>() {
            @Override
            public Collection<String> call(AutoCompletionBinding.ISuggestionRequest iSuggestionRequest) {
                String partialPath = iSuggestionRequest.getUserText();
                if(partialPath.length() < 3) {
                    return Collections.emptyList();
                } else {
                    return SystemEntityResolver.getResolver().getFromFilter(partialPath, SystemEntityType.EVENT).stream().map(o -> o.getPath().asString()).collect(Collectors.toList());
                }
            }
        });

        validate();
    }

    public void initialiseSchedulingRequest(SchedulingRequest request) {
        resourcesText.setText(formatToString(request.getResources()));
        sourceText.setText(request.getSource());
        taskExternalIdText.setText(request.getExternalId());
        expectedDurationText.setText(String.valueOf(request.getExpectedDuration().toSeconds()));
        conflictChoice.getSelectionModel().select(request.getConflictStrategy().ordinal());
        if(request.getLatestInvocationTime() != null) {
            latestExecutionCheckbox.setSelected(true);
            latestExecutionDatePicker.setValue(LocalDate.ofInstant(request.getLatestInvocationTime(), ZoneId.of("UTC")));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                    .withZone(ZoneId.of("UTC"));
            latestExecutionTimeText.setText(formatter.format(request.getLatestInvocationTime()));
        } else {
            latestExecutionCheckbox.setSelected(false);
        }
        if(request.getTrigger() instanceof NowSchedulingTrigger) {
            nowTimeRadio.setSelected(true);
        } else if(request.getTrigger() instanceof AbsoluteTimeSchedulingTrigger) {
            AbsoluteTimeSchedulingTrigger tr = (AbsoluteTimeSchedulingTrigger) request.getTrigger();
            absoluteDatePicker.setValue(LocalDate.ofInstant(tr.getReleaseTime(), ZoneId.of("UTC")));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                    .withZone(ZoneId.of("UTC"));
            absoluteTimeText.setText(formatter.format(tr.getReleaseTime()));
            absoluteTimeRadio.setSelected(true);
        } else if(request.getTrigger() instanceof RelativeTimeSchedulingTrigger) {
            RelativeTimeSchedulingTrigger tr = (RelativeTimeSchedulingTrigger) request.getTrigger();
            externalIdText.setText(formatToString(tr.getPredecessors()));
            relativeTimeText.setText(String.valueOf(tr.getDelayTime()));
            relativeTimeRadio.setSelected(true);
        } else if(request.getTrigger() instanceof EventBasedSchedulingTrigger) {
            eventPathText.setText(((EventBasedSchedulingTrigger) request.getTrigger()).getEvent().asString());
            protectionTimeText.setText(String.valueOf (((EventBasedSchedulingTrigger) request.getTrigger()).getProtectionTime() / 1000));
            enabledCheckBox.setSelected(((EventBasedSchedulingTrigger) request.getTrigger()).isEnabled());
            eventDrivenRadio.setSelected(true);
        }
        validate();
    }

    private String formatToString(Collection<?> data) {
        StringBuilder sb = new StringBuilder();
        for(Object s : data) {
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
        if (taskExternalIdText.getText().isBlank() || getTaskExternalId() == null) {
            setError(taskExternalIdText, true);
            errorFound = true;
        } else {
            setError(taskExternalIdText, false);
        }
        if (expectedDurationText.getText().isBlank() || getExpectedDuration() == null) {
            setError(expectedDurationText, true);
            errorFound = true;
        } else {
            setError(expectedDurationText, false);
        }
        if(latestExecutionCheckbox.isSelected()) {
            if(latestExecutionDatePicker.getValue() == null) {
                setError(latestExecutionDatePicker, true);
                errorFound = true;
            } else {
                setError(latestExecutionDatePicker, false);
            }
            if (latestExecutionTimeText.getText().isBlank() || getLatestExecutionTime() == null) {
                setError(latestExecutionTimeText, true);
                errorFound = true;
            } else {
                setError(latestExecutionTimeText, false);
            }
        } else {
            setError(latestExecutionDatePicker, false);
            setError(latestExecutionTimeText, false);
        }
        if(absoluteTimeRadio.isSelected()) {
            if(absoluteDatePicker.getValue() == null) {
                setError(absoluteDatePicker, true);
                errorFound = true;
            } else {
                setError(absoluteDatePicker, false);
            }
            if (absoluteTimeText.getText().isBlank() || getAbsoluteTime() == null) {
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
            if (relativeTimeText.getText().isBlank() || getRelativeTime() == null) {
                setError(relativeTimeText, true);
                errorFound = true;
            } else {
                setError(relativeTimeText, false);
            }
            if (externalIdText.getText().isBlank() || getRelativeTriggerExternalIds() == null) {
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
            if (protectionTimeText.getText().isBlank() || getProtectionTime() == null) {
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

    public Integer getProtectionTime() {
        try {
            return Integer.parseInt(protectionTimeText.getText()) * 1000;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getTaskExternalId() {
        try {
            return taskExternalIdText.getText();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Set<String> getRelativeTriggerExternalIds() {
        String[] spl = externalIdText.getText().split(",", -1);
        return new LinkedHashSet<>(Arrays.asList(spl));
    }

    public Duration getExpectedDuration() {
        try {
            return Duration.ofSeconds(Integer.parseInt(expectedDurationText.getText()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Integer getRelativeTime() {
        try {
            return Integer.parseInt(relativeTimeText.getText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public LocalTime getLatestExecutionTime() {
        try {
            return LocalTime.parse(latestExecutionTimeText.getText());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public LocalTime getAbsoluteTime() {
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

    public SchedulingRequest buildRequest(ActivityRequest request) {
        String source = sourceText.getText();
        Set<String> resources = getResources();
        Instant lastExecTime = this.latestExecutionCheckbox.isSelected() ? LocalDateTime.of(this.latestExecutionDatePicker.getValue(), getLatestExecutionTime()).toInstant(ZoneOffset.UTC) : null;
        Duration duration = getExpectedDuration();
        return new SchedulingRequest(request, resources, source, getTaskExternalId(), buildTrigger(), lastExecTime, ConflictStrategy.values()[conflictChoice.getSelectionModel().getSelectedIndex()], duration);
    }

    public CreationConflictStrategy getCreationStrategy() {
        return creationChoice.getSelectionModel().getSelectedItem();
    }

    private AbstractSchedulingTrigger buildTrigger() {
        if(nowTimeRadio.isSelected()) {
            return new NowSchedulingTrigger();
        } if(absoluteTimeRadio.isSelected()) {
            Instant execTime = LocalDateTime.of(this.absoluteDatePicker.getValue(), getAbsoluteTime()).toInstant(ZoneOffset.UTC);
            return new AbsoluteTimeSchedulingTrigger(execTime);
        } else if(relativeTimeRadio.isSelected()) {
            return new RelativeTimeSchedulingTrigger(getRelativeTriggerExternalIds(), getRelativeTime());
        } else if(eventDrivenRadio.isSelected()) {
            return new EventBasedSchedulingTrigger(SystemEntityPath.fromString(eventPathText.getText()), getProtectionTime(), enabledCheckBox.isSelected());
        } else {
            throw new IllegalStateException("None of the supported triggers can be derived");
        }
    }

    public void setDuration(Duration expectedDuration) {
        if(expectedDuration != null) {
            expectedDurationText.setText(String.valueOf(expectedDuration.toSeconds()));
        }
    }
}
