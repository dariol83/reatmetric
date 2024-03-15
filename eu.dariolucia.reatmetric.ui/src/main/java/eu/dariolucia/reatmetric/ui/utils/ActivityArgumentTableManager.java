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

package eu.dariolucia.reatmetric.ui.utils;

import eu.dariolucia.reatmetric.api.activity.AbstractActivityArgumentDescriptor;
import eu.dariolucia.reatmetric.api.activity.ActivityArrayArgumentDescriptor;
import eu.dariolucia.reatmetric.api.activity.ActivityDescriptor;
import eu.dariolucia.reatmetric.api.activity.ActivityPlainArgumentDescriptor;
import eu.dariolucia.reatmetric.api.processing.input.*;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import org.controlsfx.control.ToggleSwitch;

import java.util.*;

public class ActivityArgumentTableManager {

    private static final String CSS_ACTIVITY_ARGUMENT_FIXED = "x-reatmetric-activity-argument-fixed";
    private static final String CSS_ACTIVITY_ARGUMENT_NOT_READY = "x-reatmetric-activity-argument-not-ready";

    private TreeTableView<ArgumentBean> table;

    private TreeTableColumn<ArgumentBean, String> nameCol;
    private TreeTableColumn<ArgumentBean, Object> rawValueCol;
    private TreeTableColumn<ArgumentBean, Object> engValueCol;
    private TreeTableColumn<ArgumentBean, String> unitCol;

    // The top level arguments: nested arguments from array records do no appear here
    private Map<String, TreeItem<ArgumentBean>> name2item = new LinkedHashMap<>();

    private final SimpleBooleanProperty argumentTableValid = new SimpleBooleanProperty(false);

    public ActivityArgumentTableManager(ActivityDescriptor descriptor, ActivityRequest request) {
        // Create table and columns
        table = new TreeTableView<>();
        nameCol = new TreeTableColumn<>("Name");
        nameCol.setPrefWidth(130);
        nameCol.setReorderable(false);
        nameCol.setSortable(false);
        rawValueCol = new TreeTableColumn<>("Raw Value");
        rawValueCol.setPrefWidth(130);
        rawValueCol.setReorderable(false);
        rawValueCol.setSortable(false);
        engValueCol = new TreeTableColumn<>("Engineering Value");
        engValueCol.setPrefWidth(130);
        engValueCol.setReorderable(false);
        engValueCol.setSortable(false);
        unitCol = new TreeTableColumn<>("Unit");
        unitCol.setPrefWidth(50);
        unitCol.setReorderable(false);
        unitCol.setSortable(false);
        table.getColumns().addAll(nameCol, rawValueCol, engValueCol, unitCol);

        // Configure table to be not editable
        table.setEditable(false);
        // Disallow cell selection
        table.getSelectionModel().cellSelectionEnabledProperty().set(false);
        // Set root
        table.setShowRoot(false);
        table.setRoot(new TreeItem<>());

        // Configure the columns for which we want the editing feature
        nameCol.setEditable(false);
        rawValueCol.setEditable(false);
        engValueCol.setEditable(false);
        unitCol.setEditable(false);

        // Configure how the values of each cell shall be retrieved
        nameCol.setCellValueFactory((a) -> a.getValue().getValue().nameProperty());
        rawValueCol.setCellValueFactory(a -> a.getValue().getValue().rawValueProperty());
        engValueCol.setCellValueFactory(a -> a.getValue().getValue().engValueProperty());
        unitCol.setCellValueFactory((a) -> a.getValue().getValue().unitProperty());

        this.nameCol.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                TreeTableRow<ArgumentBean> currentRow = getTreeTableRow();
                if(currentRow != null && currentRow.getTreeItem() != null) {
                    if (currentRow.getTreeItem().getValue().isFixed()) {
                        currentRow.getStyleClass().add(CSS_ACTIVITY_ARGUMENT_FIXED);
                        currentRow.getStyleClass().remove(CSS_ACTIVITY_ARGUMENT_NOT_READY);
                    } else if (!currentRow.getTreeItem().getValue().readyProperty().get()) {
                        currentRow.getStyleClass().remove(CSS_ACTIVITY_ARGUMENT_FIXED);
                        currentRow.getStyleClass().add(CSS_ACTIVITY_ARGUMENT_NOT_READY);
                    } else {
                        currentRow.getStyleClass().remove(CSS_ACTIVITY_ARGUMENT_FIXED);
                        currentRow.getStyleClass().remove(CSS_ACTIVITY_ARGUMENT_NOT_READY);
                    }
                }
            }
        });

        rawValueCol.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty && !isEmpty()) {
                    setText(ValueUtil.toString(item));
                } else {
                    setText("");
                    setGraphic(null);
                }
            }
        });
        engValueCol.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty && !isEmpty()) {
                    setText(ValueUtil.toString(item));
                } else {
                    setText("");
                    setGraphic(null);
                }
            }
        });

        // Add context menu for editing
        table.setRowFactory( tv -> {
            TreeTableRow<ArgumentBean> row = new TreeTableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty()) ) {
                    editCurrentSelection();
                }
            });
            return row ;
        });
        // Initialise table with the data
        FxUtils.runLater(() -> initialise(descriptor, request));
    }

    private void initialise(ActivityDescriptor descriptor, ActivityRequest request) {
        // Create a tree item one by one for each argument in the descriptor: in case you spot an array, add it and track back
        // the argument that is the expander for such array and remember it, i.e. add a listener to autotrigger the array expansion/shrink
        // when that argument is modified. This is a dynamic thing, that must work also for sub-arrays.
        for (AbstractActivityArgumentDescriptor a : descriptor.getArgumentDescriptors()) {
            TreeItem<ArgumentBean> treeItem = new TreeItem<>(new ArgumentBean(a));
            table.getRoot().getChildren().add(treeItem);
            name2item.put(a.getName(), treeItem);
            if (a instanceof ActivityArrayArgumentDescriptor) {
                // Look for the expander and register an action to be done, when the value of the expander changes
                ActivityArrayArgumentDescriptor arrayArg = (ActivityArrayArgumentDescriptor) a;
                String expander = arrayArg.getExpansionArgument();
                // The expander is at the same level of this tree item
                TreeItem<ArgumentBean> expanderItem = lookForExpander(treeItem, expander);
                expanderItem.getValue().rawValueProperty().addListener((obj, oldV, newV) -> updateElementList(expanderItem, treeItem, arrayArg, false));
                expanderItem.getValue().engValueProperty().addListener((obj, oldV, newV) -> updateElementList(expanderItem, treeItem, arrayArg, true));
            }
        }

        // After that, initialise the tree with the request if there is any
        if (request != null) {
            for (AbstractActivityArgument arg : request.getArguments()) {
                TreeItem<ArgumentBean> item = name2item.get(arg.getName());
                if (arg instanceof PlainActivityArgument) {
                    item.getValue().updateValues(((PlainActivityArgument) arg).isEngineering(), ((PlainActivityArgument) arg).getRawValue(), ((PlainActivityArgument) arg).getEngValue());
                } else if (arg instanceof ArrayActivityArgument) {
                    // At this point, the expansion should have been already happened, so the records of the array should be already created
                    initialiseRecords(item, ((ArrayActivityArgument) arg).getRecords());
                }
            }
        }

        table.refresh();
        FxUtils.runLater(this::recheckArguments);
    }

    private void initialiseRecords(TreeItem<ArgumentBean> item, List<ArrayActivityArgumentRecord> records) {
        // item should already contain the records, which must be initialised by the contents of the records list
        if(item.getChildren().size() != records.size()) {
            throw new IllegalStateException("Cannot initialise records of " + item.getValue().getName() + ": expected records are " + records.size() + ", but found " + item.getChildren().size());
        }
        for(int i = 0; i < records.size(); ++i) {
            TreeItem<ArgumentBean> record = item.getChildren().get(i);
            ArrayActivityArgumentRecord valueRecord = records.get(i);
            initialiseRecord(record, valueRecord);
        }
    }

    private void initialiseRecord(TreeItem<ArgumentBean> record, ArrayActivityArgumentRecord valueRecord) {
        for (AbstractActivityArgument arg : valueRecord.getElements()) {
            TreeItem<ArgumentBean> item = lookByNameInRecord(arg.getName(), record);
            if (arg instanceof PlainActivityArgument) {
                item.getValue().updateValues(((PlainActivityArgument) arg).isEngineering(), ((PlainActivityArgument) arg).getRawValue(), ((PlainActivityArgument) arg).getEngValue());
            } else if (arg instanceof ArrayActivityArgument) {
                // At this point, the expansion should have been already happened, so the records of the array should be already created
                initialiseRecords(item, ((ArrayActivityArgument) arg).getRecords());
            }
        }
    }

    private TreeItem<ArgumentBean> lookByNameInRecord(String name, TreeItem<ArgumentBean> record) {
        for(TreeItem<ArgumentBean> ti : record.getChildren()) {
            if(ti.getValue().getName().equals(name)) {
                return ti;
            }
        }
        throw new IllegalStateException("Cannot retrieve element name " + name + " from record " + record.getValue().getName() + " of " + record.getParent().getValue().getName());
    }

    private void updateElementList(TreeItem<ArgumentBean> expanderTreeItem, TreeItem<ArgumentBean> arrayTreeItem, ActivityArrayArgumentDescriptor arrayArg, boolean isEngUpdated) {
        // When this method is called, we need to look at the expanderTreeItem value (raw or eng, depending on the selection) and check
        // what we have to do with the children of arrayTreeItem, i.e. how many records we need to create or remove

        // The following double block ensured that there is no Null Pointer exception computed in the middle of an update, due to the way the ArgumentBean updates the value
        if(isEngUpdated && !expanderTreeItem.getValue().isUseEng()) {
            return;
        }
        if(!isEngUpdated && expanderTreeItem.getValue().isUseEng()) {
            return;
        }
        int currentValue = expanderTreeItem.getValue().isUseEng() ? ((Number)expanderTreeItem.getValue().getEngValue()).intValue() : ((Number)expanderTreeItem.getValue().getRawValue()).intValue();
        if(currentValue != arrayTreeItem.getChildren().size()) {
            if(currentValue < arrayTreeItem.getChildren().size()) {
                // We need to remove items
                arrayTreeItem.getChildren().remove(currentValue, arrayTreeItem.getChildren().size());
            } else {
                // We need to add items
                int itemsToAdd = currentValue - arrayTreeItem.getChildren().size();
                for(int i = 0; i < itemsToAdd; ++i) {
                    TreeItem<ArgumentBean> record = createNewRecord(arrayTreeItem.getChildren().size(), arrayArg);
                    arrayTreeItem.getChildren().add(record);
                }
            }
        }
        table.refresh();
        FxUtils.runLater(this::recheckArguments);
    }

    private TreeItem<ArgumentBean> createNewRecord(int position, ActivityArrayArgumentDescriptor descriptor) {
        TreeItem<ArgumentBean> toCreate = new TreeItem<>(new ArgumentBean("Record " + position));
        // Create the contents of the record based on the arrayArg defined structure
        for (AbstractActivityArgumentDescriptor a : descriptor.getElements()) {
            TreeItem<ArgumentBean> treeItem = new TreeItem<>(new ArgumentBean(a));
            toCreate.getChildren().add(treeItem);
            if (a instanceof ActivityArrayArgumentDescriptor) {
                // Look for the expander and register an action to be done, when the value of the expander changes
                ActivityArrayArgumentDescriptor arrayArg = (ActivityArrayArgumentDescriptor) a;
                String expander = arrayArg.getExpansionArgument();
                // The expander is at the same level of this tree item
                TreeItem<ArgumentBean> expanderItem = lookForExpander(treeItem, expander);
                expanderItem.getValue().rawValueProperty().addListener((obj, oldV, newV) -> updateElementList(expanderItem, treeItem, arrayArg, false));
                expanderItem.getValue().engValueProperty().addListener((obj, oldV, newV) -> updateElementList(expanderItem, treeItem, arrayArg, true));
            }
        }
        return toCreate;
    }

    private TreeItem<ArgumentBean> lookForExpander(TreeItem<ArgumentBean> treeItem, String expander) {
        // The expander is one sibling of the treeItem, it should actually be the preceeding one but that might not be the case
        List<TreeItem<ArgumentBean>> siblings = treeItem.getParent().getChildren();
        int pos = siblings.indexOf(treeItem);
        for(int i = pos - 1; i >= 0; --i) {
            TreeItem<ArgumentBean> toCheck = siblings.get(i);
            if(toCheck.getValue().getName().equals(expander)) {
                // Found
                return toCheck;
            }
        }
        throw new IllegalStateException("Cannot retrieve expander " + expander + " for argument " + treeItem.getValue().getName());
    }

    public TreeTableView<ArgumentBean> getTable() {
        return table;
    }

    public void editCurrentSelection() {
        TreeItem<ArgumentBean> selectedItem = table.getSelectionModel().getSelectedItem();
        if(selectedItem == null) {
            return;
        }
        if(selectedItem.getValue().isFixed()) {
            return;
        }
        if(selectedItem.getValue().getDescriptor() == null) {
            return;
        }
        if(!(selectedItem.getValue().getDescriptor() instanceof ActivityPlainArgumentDescriptor)) {
            return;
        }
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Edit " + selectedItem.getValue().getName());
        d.initModality(Modality.APPLICATION_MODAL);
        d.initOwner(table.getScene().getWindow());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        ActivityInvocationArgumentLine line = new ActivityInvocationArgumentLine((ActivityPlainArgumentDescriptor) selectedItem.getValue().getDescriptor(), selectedItem.getValue());

        d.getDialogPane().setContent(line.getNode());
        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.disableProperty().bind(line.valid.not());
        Optional<ButtonType> result = d.showAndWait();
        if(result.isPresent() && result.get().equals(ButtonType.OK)) {
            line.updateArgumentBean();
        }
    }

    private void recheckArguments() {
        try {
            buildArgumentList();
            argumentTableValid.set(true);
        } catch (Exception e) {
            // If you cannot build the arguments, then there is something wrong
            argumentTableValid.set(false);
        }
    }

    public SimpleBooleanProperty argumentTableValidProperty() {
        return argumentTableValid;
    }

    public List<AbstractActivityArgument> buildArgumentList() {
        List<AbstractActivityArgument> args = new LinkedList<>();
        for(TreeItem<ArgumentBean> item : table.getRoot().getChildren()) {
            AbstractActivityArgument arg = buildArgument(item);
            args.add(arg);
        }
        return args;
    }

    private AbstractActivityArgument buildArgument(TreeItem<ArgumentBean> item) {
        ArgumentBean ab = item.getValue();
        if(ab.getDescriptor() instanceof ActivityPlainArgumentDescriptor) {
            return buildPlainArgument(ab);
        } else if(ab.getDescriptor() instanceof ActivityArrayArgumentDescriptor) {
            return buildArrayArgument(ab, item);
        } else {
            throw new IllegalStateException("Cannot process item " + item.getValue());
        }
    }

    private ArrayActivityArgument buildArrayArgument(ArgumentBean ab, TreeItem<ArgumentBean> item) {
        List<ArrayActivityArgumentRecord> recordList = new LinkedList<>();
        for(TreeItem<ArgumentBean> record : item.getChildren()) {
            List<AbstractActivityArgument> elems = new LinkedList<>();
            for(TreeItem<ArgumentBean> recordElement : record.getChildren()) {
                ArgumentBean elementValue = recordElement.getValue();
                if(elementValue.getDescriptor() instanceof ActivityPlainArgumentDescriptor) {
                    elems.add(buildPlainArgument(elementValue));
                } else if(ab.getDescriptor() instanceof ActivityArrayArgumentDescriptor) {
                    elems.add(buildArrayArgument(elementValue, recordElement));
                } else {
                    throw new IllegalStateException("Cannot process item " + item.getValue());
                }
            }
            ArrayActivityArgumentRecord rec = new ArrayActivityArgumentRecord(elems);
            recordList.add(rec);
        }
        return new ArrayActivityArgument(ab.getName(), recordList);
    }

    private PlainActivityArgument buildPlainArgument(ArgumentBean ab) {
        if(ab.getRawValue() == null && ab.getEngValue() == null) {
            throw new IllegalStateException("Argument " + ab.getName() + " does not have a value set");
        }
        return new PlainActivityArgument(ab.getName(), ab.getRawValue(), ab.getEngValue(), ab.isUseEng());
    }

    private class ArgumentBean {
        private final AbstractActivityArgumentDescriptor descriptor;

        private final SimpleStringProperty name;
        private final SimpleIntegerProperty groupCounter;
        private final SimpleObjectProperty<Object> rawValue;
        private final SimpleObjectProperty<Object> engValue;
        private final SimpleStringProperty unit;
        private final SimpleBooleanProperty useEng;
        private final SimpleBooleanProperty fixed;
        private final SimpleBooleanProperty ready;

        public ArgumentBean(String labelName) {
            name = new SimpleStringProperty(labelName);
            groupCounter = new SimpleIntegerProperty();
            rawValue = new SimpleObjectProperty<>();
            engValue = new SimpleObjectProperty<>();
            unit = new SimpleStringProperty();
            useEng = new SimpleBooleanProperty();
            fixed = new SimpleBooleanProperty(true);
            ready = new SimpleBooleanProperty(false);
            this.descriptor = null;
        }

        public ArgumentBean(AbstractActivityArgumentDescriptor descriptor) {
            name = new SimpleStringProperty();
            groupCounter = new SimpleIntegerProperty();
            rawValue = new SimpleObjectProperty<>();
            engValue = new SimpleObjectProperty<>();
            unit = new SimpleStringProperty();
            useEng = new SimpleBooleanProperty();
            fixed = new SimpleBooleanProperty();
            this.descriptor = descriptor;
            name.set(descriptor.getName());
            if(descriptor instanceof ActivityPlainArgumentDescriptor) {
                ActivityPlainArgumentDescriptor dd = (ActivityPlainArgumentDescriptor) descriptor;
                unit.set(dd.getUnit());
                fixed.set(dd.isFixed());
                if(dd.isDefaultValuePresent()) {
                    if(dd.getRawDefaultValue() != null) {
                        rawValueProperty().set(dd.getRawDefaultValue());
                        useEng.set(false);
                    } else if(dd.getEngineeringDefaultValue() != null) {
                        engValueProperty().set(dd.getEngineeringDefaultValue());
                        useEng.set(true);
                    }
                }
            } else {
                fixed.set(true);
            }
            ready = new SimpleBooleanProperty(descriptor instanceof ActivityArrayArgumentDescriptor ||
                    (descriptor instanceof ActivityPlainArgumentDescriptor && ((isUseEng() && engValueProperty().get() != null) || (!isUseEng() && rawValueProperty().get() != null))));
        }

        public SimpleBooleanProperty readyProperty() {
            return ready;
        }

        public AbstractActivityArgumentDescriptor getDescriptor() {
            return descriptor;
        }

        public String getName() {
            return name.get();
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public Object getRawValue() {
            return rawValue.get();
        }

        public SimpleObjectProperty<Object> rawValueProperty() {
            return rawValue;
        }

        public Object getEngValue() {
            return engValue.get();
        }

        public SimpleObjectProperty<Object> engValueProperty() {
            return engValue;
        }

        public SimpleStringProperty unitProperty() {
            return unit;
        }

        public boolean isUseEng() {
            return useEng.get();
        }

        public SimpleBooleanProperty useEngProperty() {
            return useEng;
        }

        public boolean isFixed() {
            return fixed.get();
        }

        public SimpleIntegerProperty groupCounterProperty() {
            return groupCounter;
        }

        public void updateValues(boolean engineering, Object rawValue, Object engValue) {
            useEngProperty().set(engineering);
            rawValueProperty().set(rawValue);
            engValueProperty().set(engValue);
            table.refresh();
            ready.set(descriptor instanceof ActivityArrayArgumentDescriptor ||
                    (descriptor instanceof ActivityPlainArgumentDescriptor && ((isUseEng() && engValueProperty().get() != null) || (!isUseEng() && rawValueProperty().get() != null))));
            FxUtils.runLater(ActivityArgumentTableManager.this::recheckArguments);
        }

        @Override
        public String toString() {
            return "ArgumentBean{" +
                    "name=" + name +
                    '}';
        }
    }

    private static class ActivityInvocationArgumentLine {

        private final ActivityPlainArgumentDescriptor descriptor;
        private final ArgumentBean input;

        private VBox container;

        private final ReatmetricValidationSupport validationSupport = new ReatmetricValidationSupport();
        private final SimpleBooleanProperty valid = new SimpleBooleanProperty(false);
        private Control rawValueControl;
        private Control engValueControl;
        private RadioButton rawSelection;
        private RadioButton engSelection;

        public ActivityInvocationArgumentLine(ActivityPlainArgumentDescriptor descriptor, ArgumentBean input) {
            this.descriptor = descriptor;
            this.input = input;
            this.valid.bind(this.validationSupport.validProperty());
            initialiseNode();
        }

        private void initialiseNode() {
            container = new VBox();
            container.setSpacing(8);
            container.setPadding(new Insets(8));

            if(descriptor.getDescription() != null && !descriptor.getDescription().isBlank()) {
                Label descLabel = new Label(descriptor.getDescription());
                descLabel.setWrapText(true);
                container.getChildren().add(descLabel);
            }

            HBox line1 = new HBox();
            line1.setSpacing(8);

            HBox line2 = new HBox();
            line2.setSpacing(8);

            ToggleGroup rawEngToggleGroup = new ToggleGroup();

            // Raw value
            rawSelection = new RadioButton("Raw Value");
            rawSelection.setPrefWidth(120);
            rawSelection.setToggleGroup(rawEngToggleGroup);
            rawSelection.setTextAlignment(TextAlignment.LEFT);
            line1.getChildren().add(rawSelection);
            rawValueControl = ValueControlUtil.buildValueControl(validationSupport,
                    descriptor.getRawDataType(),
                    input != null ? input.getRawValue() : null,
                    descriptor.getRawDefaultValue(),
                    descriptor.isFixed(),
                    descriptor.getExpectedRawValues());
            rawValueControl.setPrefWidth(150);
            line1.getChildren().add(rawValueControl);
            Label emptyLabel = new Label("");
            emptyLabel.setPrefWidth(70);
            line1.getChildren().add(emptyLabel);
            container.getChildren().add(line1);

            // Eng. value
            engSelection = new RadioButton("Eng. Value");
            engSelection.setPrefWidth(120);
            engSelection.setToggleGroup(rawEngToggleGroup);
            engSelection.setTextAlignment(TextAlignment.LEFT);
            line2.getChildren().add(engSelection);
            engValueControl = ValueControlUtil.buildValueControl(validationSupport,
                    descriptor.getEngineeringDataType(),
                    input != null ? input.getEngValue() : null,
                    descriptor.getEngineeringDefaultValue(),
                    descriptor.isFixed(),
                    descriptor.getExpectedEngineeringValues());
            engValueControl.setPrefWidth(150);
            line2.getChildren().add(engValueControl);
            // Unit
            Label unitLbl = new Label(Objects.toString(descriptor.getUnit(), ""));
            unitLbl.setPrefWidth(70);
            line2.getChildren().add(unitLbl);
            container.getChildren().add(line2);

            // Raw/Eng value selection
            rawValueControl.disableProperty().bind(rawSelection.selectedProperty().not());
            engValueControl.disableProperty().bind(engSelection.selectedProperty().not());

            if(input != null) {
                rawSelection.setSelected(!input.isUseEng());
                engSelection.setSelected(input.isUseEng());
            } else if(descriptor.isDefaultValuePresent()) {
                rawSelection.setSelected(descriptor.getRawDefaultValue() != null);
                engSelection.setSelected(descriptor.getEngineeringDefaultValue() != null);
            } else {
                rawSelection.setSelected(false);
                engSelection.setSelected(true);
            }

            FxUtils.runLater(() -> {
                focusOnControl(rawSelection.isSelected(), rawValueControl, engValueControl);
            });
        }

        private void focusOnControl(boolean rawValueSelected, Control rawValueControl, Control engValueControl) {
            if(rawValueSelected) {
                rawValueControl.requestFocus();
                if(rawValueControl instanceof TextField) {
                    ((TextField) rawValueControl).selectAll();
                }
            } else {
                engValueControl.requestFocus();
                if(engValueControl instanceof TextField) {
                    ((TextField) engValueControl).selectAll();
                }
            }
        }

        public VBox getNode() {
            return container;
        }

        public void updateArgumentBean() {
            input.updateValues(engSelection.isSelected(),
                    rawSelection.isSelected() ? buildObject(descriptor.getRawDataType(), rawValueControl) : null,
                    engSelection.isSelected() ? buildObject(descriptor.getEngineeringDataType(), engValueControl) : null);
        }

        private Object buildObject(ValueTypeEnum type, Control control) {
            if(control instanceof TextField) {
                return ValueUtil.parse(type, ((TextField) control).getText());
            } else if(control instanceof ToggleSwitch) {
                return ((ToggleSwitch) control).isSelected();
            } else if(control instanceof ComboBox) {
                return ((ComboBox<?>) control).getSelectionModel().getSelectedItem();
            } else {
                return null;
            }
        }
    }
}
