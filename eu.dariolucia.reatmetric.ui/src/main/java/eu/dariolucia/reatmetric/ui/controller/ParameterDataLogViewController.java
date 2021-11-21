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

import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.InstantCellFactory;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.paint.Color;

import java.net.URL;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class ParameterDataLogViewController extends AbstractDataItemLogViewController<ParameterData, ParameterDataFilter>
		implements IParameterDataSubscriber {

	@FXML
	private TableColumn<ParameterData, String> nameCol;
	@FXML
	private TableColumn<ParameterData, String> engValueCol;
	@FXML
	private TableColumn<ParameterData, String> sourceValueCol;
	@FXML
	private TableColumn<ParameterData, Validity> validityCol;
	@FXML
	private TableColumn<ParameterData, AlarmState> alarmStateCol;
	@FXML
	private TableColumn<ParameterData, Instant> genTimeCol;
	@FXML
	private TableColumn<ParameterData, Instant> recTimeCol;
	@FXML
	private TableColumn<ParameterData, String> parentCol;

	@Override
	protected void doInitialize(URL url, ResourceBundle rb) {
		super.doInitialize(url, rb);
		this.nameCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getName()));
		this.engValueCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(ValueUtil.toString(o.getValue().getEngValue())));
		this.sourceValueCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(ValueUtil.toString(o.getValue().getSourceValue())));
		this.validityCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getValidity()));
		this.genTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getGenerationTime()));
		this.recTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getReceptionTime()));
		this.alarmStateCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getAlarmState()));
		this.parentCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getPath().getParent().asString()));

		this.genTimeCol.setCellFactory(InstantCellFactory.instantCellFactory());
		this.recTimeCol.setCellFactory(InstantCellFactory.instantCellFactory());

		this.validityCol.setCellFactory(column -> new TableCell<>() {
			@Override
			protected void updateItem(Validity item, boolean empty) {
				super.updateItem(item, empty);
				if (item != null && !empty && !isEmpty()) {
					setText(item.name());
					switch (item) {
						case DISABLED:
							setTextFill(Color.GRAY);
							break;
						case INVALID:
							setTextFill(Color.RED);
							break;
						case ERROR:
							setTextFill(Color.DARKRED);
							break;
						case UNKNOWN:
							setTextFill(Color.CHOCOLATE);
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
		this.alarmStateCol.setCellFactory(column -> new TableCell<>() {
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
						case IGNORED:
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

	@Override
	public void dataItemsReceived(List<ParameterData> messages) {
		informDataItemsReceived(messages);
	}

	@Override
	protected void doServiceSubscribe(ParameterDataFilter selectedFilter) throws ReatmetricException {
		try {
			ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().subscribe(this, selectedFilter);
		} catch (RemoteException e) {
			throw new ReatmetricException(e);
		}
	}

	@Override
	protected void doServiceUnsubscribe() throws ReatmetricException {
		try {
			ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().unsubscribe(this);
		} catch (RemoteException e) {
			throw new ReatmetricException(e);
		}
	}

	@Override
	protected List<ParameterData> doRetrieve(ParameterData om, int n, RetrievalDirection direction, ParameterDataFilter filter)
			throws ReatmetricException {
		try {
			return ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().retrieve(om, n, direction, filter);
		} catch (RemoteException e) {
			throw new ReatmetricException(e);
		}
	}

	@Override
	protected List<ParameterData> doRetrieve(Instant selectedTime, int n, RetrievalDirection direction,
											 ParameterDataFilter filter) throws ReatmetricException {
		try {
			return ReatmetricUI.selectedSystem().getSystem().getParameterDataMonitorService().retrieve(selectedTime, n,
					direction, filter);
		} catch (RemoteException e) {
			throw new ReatmetricException(e);
		}
	}

	@Override
	protected Instant doGetGenerationTime(ParameterData om) {
		return om.getGenerationTime();
	}

	@Override
	protected URL doGetFilterWidget() {
		return getClass().getClassLoader()
				.getResource("eu/dariolucia/reatmetric/ui/fxml/ParameterDataFilterWidget.fxml");
	}

	@Override
	protected String doGetComponentId() {
		return "ParameterDataLogView";
	}

	@FXML
	private void locateItemAction(ActionEvent actionEvent) {
		ParameterData ed = this.dataItemTableView.getSelectionModel().getSelectedItem();
		if(ed != null) {
			MainViewController.instance().getModelController().locate(ed.getPath());
		}
	}
}
