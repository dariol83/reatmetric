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
import javafx.stage.Modality;
import org.controlsfx.control.ToggleSwitch;

import java.util.*;

public class ActivityArgumentTableManager {

    private TreeTableView<ArgumentBean> table;

    private TreeTableColumn<ArgumentBean, String> nameCol;
    private TreeTableColumn<ArgumentBean, Number> groupCounterCol;
    private TreeTableColumn<ArgumentBean, Object> rawValueCol;
    private TreeTableColumn<ArgumentBean, Object> engValueCol;
    private TreeTableColumn<ArgumentBean, String> unitCol;
    private TreeTableColumn<ArgumentBean, Boolean> rawEngValueCol;

    // The top level arguments: nested arguments from array records do no appear here
    private Map<String, TreeItem<ArgumentBean>> name2item = new LinkedHashMap<>();

    public ActivityArgumentTableManager(ActivityDescriptor descriptor, ActivityRequest request) {
        // Create table and columns
        table = new TreeTableView<>();
        nameCol = new TreeTableColumn<>("Name");
        nameCol.setPrefWidth(130);
        nameCol.setReorderable(false);
        nameCol.setSortable(false);
        groupCounterCol = new TreeTableColumn<>("Grp.");
        groupCounterCol.setPrefWidth(50);
        groupCounterCol.setReorderable(false);
        groupCounterCol.setSortable(false);
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
        rawEngValueCol = new TreeTableColumn<>("Use Eng.");
        rawEngValueCol.setPrefWidth(60);
        rawEngValueCol.setReorderable(false);
        rawEngValueCol.setSortable(false);
        table.getColumns().addAll(nameCol, groupCounterCol, rawValueCol, engValueCol, unitCol, rawEngValueCol);

        // Configure table to be not editable
        table.setEditable(false);
        // Disallow cell selection
        table.getSelectionModel().cellSelectionEnabledProperty().set(false);
        // Set root
        table.setShowRoot(false);
        table.setRoot(new TreeItem<>());

        // Configure the columns for which we want the editing feature
        nameCol.setEditable(false);
        groupCounterCol.setEditable(false);
        rawValueCol.setEditable(false);
        engValueCol.setEditable(false);
        unitCol.setEditable(false);
        rawEngValueCol.setEditable(false);

        // Configure how the values of each cell shall be retrieved
        nameCol.setCellValueFactory((a) -> a.getValue().getValue().nameProperty());
        groupCounterCol.setCellValueFactory((a) -> a.getValue().getValue().groupCounterProperty());
        rawValueCol.setCellValueFactory(a -> a.getValue().getValue().rawValueProperty());
        engValueCol.setCellValueFactory(a -> a.getValue().getValue().engValueProperty());
        rawEngValueCol.setCellValueFactory(a -> a.getValue().getValue().useEngProperty());
        unitCol.setCellValueFactory((a) -> a.getValue().getValue().unitProperty());

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
        Platform.runLater(() -> initialise(descriptor, request));
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
                expanderItem.getValue().rawValueProperty().addListener((obj, oldV, newV) -> updateElementList(expanderItem, treeItem, arrayArg));
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

    private void updateElementList(TreeItem<ArgumentBean> expanderTreeItem, TreeItem<ArgumentBean> arrayTreeItem, ActivityArrayArgumentDescriptor arrayArg) {
        // When this method is called, we need to look at the expanderTreeItem value (raw or eng, depending on the selection) and check
        // what we have to do with the children of arrayTreeItem, i.e. how many records we need to create or remove
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
                expanderItem.getValue().rawValueProperty().addListener((obj, oldV, newV) -> updateElementList(expanderItem, treeItem, arrayArg));
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
        return new PlainActivityArgument(ab.getName(), ab.getRawValue(), ab.getEngValue(), ab.isUseEng());
    }

    private class ArgumentBean {
        private final AbstractActivityArgumentDescriptor descriptor;

        // TODO: implement valid state, so that every time this state changes, the manager can navigate the tree and
        //  derive the final validity state of the argument set
        private final SimpleStringProperty name;
        private final SimpleIntegerProperty groupCounter;
        private final SimpleObjectProperty<Object> rawValue;
        private final SimpleObjectProperty<Object> engValue;
        private final SimpleStringProperty unit;
        private final SimpleBooleanProperty useEng;
        private final SimpleBooleanProperty fixed;

        public ArgumentBean(String labelName) {
            name = new SimpleStringProperty(labelName);
            groupCounter = new SimpleIntegerProperty();
            rawValue = new SimpleObjectProperty<>();
            engValue = new SimpleObjectProperty<>();
            unit = new SimpleStringProperty();
            useEng = new SimpleBooleanProperty();
            fixed = new SimpleBooleanProperty(true);
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
            rawValueProperty().set(rawValue);
            engValueProperty().set(engValue);
            useEngProperty().set(engineering);
            table.refresh();
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

        private HBox node;

        // TODO: implement validation and bind it to OK button on the dialog window
        private final SimpleBooleanProperty valid = new SimpleBooleanProperty(true);
        private Control rawValueControl;
        private Control engValueControl;
        private CheckBox rawEngSelection;

        public ActivityInvocationArgumentLine(ActivityPlainArgumentDescriptor descriptor, ArgumentBean input) {
            this.descriptor = descriptor;
            this.input = input;
            // this.valid.bind(this.validationSupport.invalidProperty().not());
            initialiseNode();
        }

        private void initialiseNode() {
            node = new HBox();
            node.setSpacing(8);
            node.setPadding(new Insets(8));
            // Name
            Label nameLbl = new Label(descriptor.getName());
            nameLbl.setPrefWidth(100);
            nameLbl.setTooltip(new Tooltip(descriptor.getDescription()));
            // Unit
            Label unitLbl = new Label(Objects.toString(descriptor.getUnit(), ""));
            unitLbl.setPrefWidth(70);
            // Raw value
            rawValueControl = ValueControlUtil.buildValueControl(null,
                    descriptor.getRawDataType(),
                    input != null ? input.getRawValue() : null,
                    descriptor.getRawDefaultValue(),
                    descriptor.isFixed(),
                    descriptor.getExpectedRawValues());
            rawValueControl.setPrefWidth(150);
            // Eng. value
            engValueControl = ValueControlUtil.buildValueControl(null,
                    descriptor.getEngineeringDataType(),
                    input != null ? input.getEngValue() : null,
                    descriptor.getEngineeringDefaultValue(),
                    descriptor.isFixed(),
                    descriptor.getExpectedEngineeringValues());
            engValueControl.setPrefWidth(150);
            // Raw/Eng value selection
            rawEngSelection = new CheckBox();
            rawEngSelection.setText("Use Eng.");
            rawEngSelection.setPrefWidth(90);

            SimpleBooleanProperty fixedProperty = new SimpleBooleanProperty(descriptor.isFixed());
            rawValueControl.disableProperty().bind(rawEngSelection.selectedProperty().or(fixedProperty));
            engValueControl.disableProperty().bind(rawEngSelection.selectedProperty().not().or(fixedProperty));

            if(input != null) {
                rawEngSelection.setSelected(input.isUseEng());
            } else if(descriptor.isDefaultValuePresent()) {
                if(descriptor.getEngineeringDefaultValue() != null) {
                    rawEngSelection.setSelected(true);
                } else {
                    rawEngSelection.setSelected(false);
                }
            } else {
                rawEngSelection.setSelected(true);
            }

            rawEngSelection.disableProperty().bind(fixedProperty);

            node.getChildren().addAll(nameLbl, rawValueControl, engValueControl, unitLbl, rawEngSelection);
            // validationSupport.initInitialDecoration();
        }

        public HBox getNode() {
            return node;
        }

        public void updateArgumentBean() {
            input.useEngProperty().set(rawEngSelection.isSelected());
            input.rawValueProperty().set(!rawEngSelection.isSelected() ? buildObject(descriptor.getRawDataType(), rawValueControl) : null);
            input.engValueProperty().set(rawEngSelection.isSelected() ? buildObject(descriptor.getEngineeringDataType(), engValueControl) : null);
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
