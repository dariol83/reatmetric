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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.validation.ValidationSupport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActivityArgumentTableManager {

    private TreeTableView<ArgumentBean> table;

    private TreeTableColumn<ArgumentBean, String> nameCol;
    private TreeTableColumn<ArgumentBean, Number> groupCounterCol;
    private TreeTableColumn<ArgumentBean, Object> rawValueCol;
    private TreeTableColumn<ArgumentBean, Object> engValueCol;
    private TreeTableColumn<ArgumentBean, String> unitCol;
    private TreeTableColumn<ArgumentBean, Boolean> rawEngValueCol;

    private ValidationSupport validationSupport = new ValidationSupport();

    private Map<String, TreeItem<ArgumentBean>> name2item = new LinkedHashMap<>();

    public ActivityArgumentTableManager(ActivityDescriptor descriptor, ActivityRequest request) {
        table = new TreeTableView<>();
        nameCol = new TreeTableColumn<>();
        groupCounterCol = new TreeTableColumn<>();
        rawValueCol = new TreeTableColumn<>();
        engValueCol = new TreeTableColumn<>();
        unitCol = new TreeTableColumn<>();
        rawEngValueCol = new TreeTableColumn<>();
        table.getColumns().addAll(nameCol, groupCounterCol, rawValueCol, engValueCol, unitCol, rawEngValueCol);

        table.setEditable(true);
        table.setShowRoot(false);
        table.setRoot(new TreeItem<>());

        nameCol.setEditable(false);
        groupCounterCol.setEditable(false);
        rawValueCol.setEditable(true);
        engValueCol.setEditable(true);
        unitCol.setEditable(false);
        rawEngValueCol.setEditable(true);

        nameCol.setCellValueFactory((a) -> a.getValue().getValue().nameProperty());
        groupCounterCol.setCellValueFactory((a) -> a.getValue().getValue().groupCounterProperty());
        rawValueCol.setCellValueFactory(a -> a.getValue().getValue().rawValueProperty());
        engValueCol.setCellValueFactory(a -> a.getValue().getValue().engValueProperty());
        rawEngValueCol.setCellValueFactory(a -> a.getValue().getValue().useEngProperty());
        unitCol.setCellValueFactory((a) -> a.getValue().getValue().unitProperty());

        rawEngValueCol.setCellFactory((a) -> new BooleanCell());
        rawValueCol.setCellFactory((a) -> new EditValueCell());
        engValueCol.setCellFactory((a) -> new EditValueCell());

        initialise(descriptor, request);
    }

    private void initialise(ActivityDescriptor descriptor, ActivityRequest request) {
        // Create a tree item one by one for each argument in the descriptor: in case you spot an array, add it and track back
        // the argument that is the expander for such array and remember it, i.e. add a listener to autotrigger the array expansion/shrink
        // when that argument is modified. This is a dynamic thing, that must work also for sub-arrays.
        for(AbstractActivityArgumentDescriptor a : descriptor.getArgumentDescriptors()) {
            TreeItem<ArgumentBean> treeItem = new TreeItem<>(new ArgumentBean(a));
            table.getRoot().getChildren().add(treeItem);
            name2item.put(a.getName(), treeItem);
            if(a instanceof ActivityArrayArgumentDescriptor) {
                // Look for the expander and register an action to be done, when the value of the expander changes
                ActivityArrayArgumentDescriptor arrayArg = (ActivityArrayArgumentDescriptor) a;
                String expander = arrayArg.getExpansionArgument();
                // The expander is at the same level of this tree item
                TreeItem<ArgumentBean> expanderItem = lookForExpander(treeItem, expander);
                expanderItem.getValue().rawValueProperty().addListener((obj,oldV,newV) -> updateElementList(treeItem, arrayArg));
            }
        }

        // After that, initialise the tree with the request if there is any
        if(request != null) {
            for(AbstractActivityArgument arg : request.getArguments()) {
                TreeItem<ArgumentBean> item = name2item.get(arg.getName());
                if(arg instanceof PlainActivityArgument) {
                    item.getValue().updateValues(((PlainActivityArgument) arg).isEngineering(), ((PlainActivityArgument) arg).getRawValue(), ((PlainActivityArgument) arg).getEngValue());
                } else if(arg instanceof ArrayActivityArgument) {
                    // At this point, the expansion should have been already happened, so the records of the array should be already created
                    initialiseRecords(item, ((ArrayActivityArgument) arg).getRecords());
                }
            }
        }
    }

    private void initialiseRecords(TreeItem<ArgumentBean> item, List<ArrayActivityArgumentRecord> records) {
        // TODO
    }

    private void updateElementList(TreeItem<ArgumentBean> treeItem, ActivityArrayArgumentDescriptor arrayArg) {
        // TODO
    }

    private TreeItem<ArgumentBean> lookForExpander(TreeItem<ArgumentBean> treeItem, String expander) {
        // TODO
        return null;
    }

    public TreeTableView<ArgumentBean> getTable() {
        return table;
    }

    private static class ArgumentBean {
        private final AbstractActivityArgumentDescriptor descriptor;

        private final SimpleStringProperty name;
        private final SimpleIntegerProperty groupCounter;
        private final SimpleObjectProperty<Object> rawValue;
        private final SimpleObjectProperty<Object> engValue;
        private final SimpleStringProperty unit;
        private final SimpleBooleanProperty useEng;
        private final SimpleBooleanProperty fixed;

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

        public String getUnit() {
            return unit.get();
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

        public SimpleBooleanProperty fixedProperty() {
            return fixed;
        }

        public int getGroupCounter() {
            return groupCounter.get();
        }

        public SimpleIntegerProperty groupCounterProperty() {
            return groupCounter;
        }

        public void updateValues(boolean engineering, Object rawValue, Object engValue) {
            // TODO
        }
    }

    private class BooleanCell extends TreeTableCell<ArgumentBean, Boolean> {

        private CheckBox checkBox;

        public BooleanCell() {
            checkBox = new CheckBox();
            checkBox.setDisable(true);
            checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if(isEditing()) {
                    commitEdit(newValue == null ? false : newValue);
                }
            });
            this.setGraphic(checkBox);
            this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            this.setEditable(true);
        }

        @Override
        public void startEdit() {
            super.startEdit();
            if (isEmpty()) {
                return;
            }
            checkBox.setDisable(false);
            checkBox.requestFocus();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            checkBox.setDisable(true);
        }

        public void commitEdit(Boolean value) {
            super.commitEdit(value);
            checkBox.setDisable(true);
        }

        @Override
        public void updateItem(Boolean item, boolean empty) {
            super.updateItem(item, empty);
            if (!isEmpty()) {
                checkBox.setSelected(item);
            }
        }
    }

    private class EditValueCell extends TreeTableCell<ArgumentBean, Object> {

        private Control control;
        private ValueTypeEnum type;
        private boolean initialised = false;

        public EditValueCell() {
            this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            this.setEditable(true);
        }

        private void addListenerForEdit(Control control) {
            if(control instanceof CheckBox) {
                ((CheckBox) control).selectedProperty().addListener((observable, oldValue, newValue) -> {
                    if(isEditing()) {
                        commitEdit(newValue);
                    }
                });
            } else if(control instanceof ToggleSwitch) {
                ((ToggleSwitch) control).selectedProperty().addListener((observable, oldValue, newValue) -> {
                    if(isEditing()) {
                        commitEdit(newValue);
                    }
                });
            } else if(control instanceof ComboBox) {
                ((ComboBox) control).getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    if(isEditing()) {
                        commitEdit(newValue);
                    }
                });
            } else if(control instanceof TextField) {
                ((TextField) control).setOnAction(e -> {
                    commitEdit(((TextField) control).getText());
                });
                control.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        cancelEdit();
                    }
                });
            }
        }

        @Override
        public void startEdit() {
            super.startEdit();
            if (isEmpty()) {
                return;
            }
            control.setDisable(false);
            control.requestFocus();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            control.setDisable(true);
        }

        public void commitEdit(Object value) {
            super.commitEdit(value);
            control.setDisable(true);
        }

        @Override
        public void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if(!initialised) {
                ArgumentBean arg = getTreeTableRow().getTreeItem().getValue();
                AbstractActivityArgumentDescriptor descr = arg.getDescriptor();
                if(descr instanceof ActivityPlainArgumentDescriptor) {
                    type = getTableColumn() == engValueCol ? ((ActivityPlainArgumentDescriptor) descr).getEngineeringDataType() : ((ActivityPlainArgumentDescriptor) descr).getRawDataType();
                    control = ValueControlUtil.buildValueControl(validationSupport,
                            type,
                            item,
                            item,
                            ((ActivityPlainArgumentDescriptor) descr).isFixed(),
                            getTableColumn() == engValueCol ? ((ActivityPlainArgumentDescriptor) descr).getExpectedEngineeringValues() : ((ActivityPlainArgumentDescriptor) descr).getExpectedRawValues());
                    control.setDisable(true);
                    addListenerForEdit(control);
                    this.setGraphic(control);
                    initialised = true;
                }
            }
            if (!isEmpty()) {
                setValue(control, item);
            }
        }

        private void setValue(Control control, Object item) {
            if(control instanceof CheckBox) {
                ((CheckBox) control).setSelected((Boolean) item);
            } else if(control instanceof ToggleSwitch) {
                ((ToggleSwitch) control).setSelected((Boolean) item);
            } else if(control instanceof ComboBox) {
                ((ComboBox) control).getSelectionModel().select(item);
            } else if(control instanceof TextField) {
                ((TextField) control).setText(ValueUtil.toString(type, item));
            }
        }
    }
}
