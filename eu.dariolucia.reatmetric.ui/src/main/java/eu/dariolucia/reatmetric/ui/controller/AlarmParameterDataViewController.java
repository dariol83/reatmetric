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

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.alarms.AlarmParameterDataFilter;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.InstantCellFactory;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class AlarmParameterDataViewController
		extends AbstractDataItemLogViewController<AlarmParameterData, AlarmParameterDataFilter>
		implements IAlarmParameterDataSubscriber {

	@FXML
	private TableColumn<AlarmParameterData, String> nameCol;
	@FXML
	private TableColumn<AlarmParameterData, String> currentValueCol;
	@FXML
	private TableColumn<AlarmParameterData, AlarmState> currentAlarmStateCol;
	@FXML
	private TableColumn<AlarmParameterData, Instant> genTimeCol;
	@FXML
	private TableColumn<AlarmParameterData, Instant> recTimeCol;
	@FXML
	private TableColumn<AlarmParameterData, String> lastNomValueCol;
	@FXML
	private TableColumn<AlarmParameterData, Instant> lastNomValueTimeCol;
	@FXML
	private TableColumn<AlarmParameterData, String> parentCol;

	@Override
	protected void doInitialize(URL url, ResourceBundle rb) {
		super.doInitialize(url, rb);
		this.nameCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getName()));
		this.currentValueCol.setCellValueFactory(
				o -> new ReadOnlyObjectWrapper<>(ValueUtil.toString(o.getValue().getCurrentValue())));
		this.currentAlarmStateCol
				.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getCurrentAlarmState()));
		this.genTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getGenerationTime()));
		this.recTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getReceptionTime()));
		this.lastNomValueCol.setCellValueFactory(
				o -> new ReadOnlyObjectWrapper<>(ValueUtil.toString(o.getValue().getLastNominalValue())));
		this.lastNomValueTimeCol
				.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getLastNominalValueTime()));
		this.parentCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getPath().getParent().asString()));

		this.genTimeCol.setCellFactory(InstantCellFactory.instantCellFactory());
		this.recTimeCol.setCellFactory(InstantCellFactory.instantCellFactory());
		this.currentAlarmStateCol.setCellFactory(column -> new TableCell<>() {
			@Override
			protected void updateItem(AlarmState item, boolean empty) {
				super.updateItem(item, empty);
				if (item != null && !empty && !isEmpty()) {
					setText(item.name());
					switch (item) {
						case ALARM:
							setTextFill(Color.RED);
							break;
						case ERROR:
							setTextFill(Color.DARKRED);
							break;
						case WARNING:
							setTextFill(Color.CHOCOLATE);
							break;
						case VIOLATED:
							setTextFill(Color.DARKGOLDENROD);
							break;
						case UNKNOWN:
						case NOT_APPLICABLE:
						case NOT_CHECKED:
							setTextFill(Color.GRAY);
							break;
						default:
							setTextFill(Color.DARKGREEN);
							break;
					}
				} else {
					setText("");
					setGraphic(null);
				}
			}
		});
	}

	@FXML
	protected void onDragOver(DragEvent event) {
		if (event.getGestureSource() != this.dataItemTableView
				&& event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.CONTAINER))) {
			event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
		}
		event.consume();
	}

	@FXML
	private void onDragEntered(DragEvent event) {
		if (event.getGestureSource() != this.dataItemTableView
				&& event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.CONTAINER))) {
		}
		event.consume();
	}

	@FXML
	private void onDragExited(DragEvent event) {
		if (event.getGestureSource() != this.dataItemTableView
				&& event.getDragboard().hasContent(SystemEntityDataFormats.getByType(SystemEntityType.CONTAINER))) {
		}
		event.consume();
	}

	@FXML
	private void onDragDropped(DragEvent event) {
		Dragboard db = event.getDragboard();
		boolean success = false;
		if (db.hasContent(SystemEntityDataFormats.CONTAINER)) {
			List<SystemEntity> entities = (List<SystemEntity>) db.getContent(SystemEntityDataFormats.CONTAINER);
			// Get the first
			AlarmParameterDataFilter filter = getCurrentFilter();
			if (filter == null) {
				filter = new AlarmParameterDataFilter(null, null,null, null);
			}
			AlarmParameterDataFilter newFilter = new AlarmParameterDataFilter(entities.get(0).getPath(), null,
					filter.getAlarmStateList(), null);
			applyFilter(newFilter);
			success = true;
		}

		event.setDropCompleted(success);
		event.consume();
	}

    @FXML
    @Override
    protected void liveToggleSelected(ActionEvent e) {
        if (this.liveTgl.isSelected()) {
        	clearTable();
        	startSubscription();
        } else {
            stopSubscription();
            // moveToTime(Instant.now(), RetrievalDirection.TO_PAST, getNumVisibleRow(), getCurrentFilter());
            updateSelectTime();
        }
    }

	@Override
	public void dataItemsReceived(List<AlarmParameterData> messages) {
		informDataItemsReceived(messages);
	}

	@Override
	protected void doServiceSubscribe(AlarmParameterDataFilter selectedFilter) throws ReatmetricException {
		ReatmetricUI.selectedSystem().getSystem().getAlarmParameterDataMonitorService().subscribe(this, selectedFilter);
	}

	@Override
	protected void doServiceUnsubscribe() throws ReatmetricException {
		ReatmetricUI.selectedSystem().getSystem().getAlarmParameterDataMonitorService().unsubscribe(this);
	}

	@Override
	protected List<AlarmParameterData> doRetrieve(AlarmParameterData om, int n, RetrievalDirection direction, AlarmParameterDataFilter filter)
			throws ReatmetricException {
		return ReatmetricUI.selectedSystem().getSystem().getAlarmParameterDataMonitorService().retrieve(om, n, direction,
				filter);
	}

	@Override
	protected List<AlarmParameterData> doRetrieve(Instant selectedTime, int n, RetrievalDirection direction,
			AlarmParameterDataFilter filter) throws ReatmetricException {
		return ReatmetricUI.selectedSystem().getSystem().getAlarmParameterDataMonitorService().retrieve(selectedTime, n,
				direction, filter);
	}

	@Override
	protected Instant doGetGenerationTime(AlarmParameterData om) {
		return om.getGenerationTime();
	}

	@Override
	protected URL doGetFilterWidget() {
		return getClass().getClassLoader()
				.getResource("eu/dariolucia/reatmetric/ui/fxml/AlarmParameterDataFilterWidget.fxml");
	}

	@Override
	protected String doGetComponentId() {
		return "AlarmParameterDataView";
	}

}
