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
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.ui.CssHandler;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.widgets.DetachedTabUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.List;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class ParameterDataLogViewController extends AbstractDataItemLogViewController<ParameterData, ParameterDataFilter>
		implements IParameterDataSubscriber {

	@FXML
	protected CheckMenuItem toggleShowToolbarItem;
	@FXML
	protected MenuItem detachMenuItem;
	@FXML
	protected ToolBar toolbar;

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

		this.genTimeCol.setCellFactory(getInstantCellCallback());
		this.recTimeCol.setCellFactory(getInstantCellCallback());

		this.nameCol.setCellFactory(getNormalTextCellCallback());
		this.engValueCol.setCellFactory(getNormalTextCellCallback());
		this.sourceValueCol.setCellFactory(getNormalTextCellCallback());
		this.parentCol.setCellFactory(getNormalTextCellCallback());

		this.validityCol.setCellFactory(zoomEnabledWrapper(column -> new TableCell<>() {
			@Override
			protected void updateItem(Validity item, boolean empty) {
				super.updateItem(item, empty);
				if (item != null && !empty && !isEmpty()) {
					setText(item.name());
					switch (item) {
						case DISABLED:
							CssHandler.updateStyleClass(this, CssHandler.CSS_VALIDITY_DISABLED);
							break;
						case INVALID:
							CssHandler.updateStyleClass(this, CssHandler.CSS_VALIDITY_INVALID);
							break;
						case ERROR:
							CssHandler.updateStyleClass(this, CssHandler.CSS_VALIDITY_ERROR);
							break;
						case UNKNOWN:
							CssHandler.updateStyleClass(this, CssHandler.CSS_VALIDITY_UNKNOWN);
							break;
						default:
							CssHandler.updateStyleClass(this, CssHandler.CSS_VALIDITY_VALID);
							break;
					}
				} else {
					CssHandler.updateStyleClass(this, null);
					setText("");
					setGraphic(null);
				}
			}
		}));
		this.alarmStateCol.setCellFactory(zoomEnabledWrapper(column -> new TableCell<>() {
			@Override
			protected void updateItem(AlarmState item, boolean empty) {
				super.updateItem(item, empty);
				if (item != null && !empty && !isEmpty()) {
					setText(item.name());
					switch (item) {
						case ALARM:
							CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_ALARM);
							break;
						case ERROR:
							CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_ERROR);
							break;
						case WARNING:
							CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_WARNING);
							break;
						case VIOLATED:
							CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_VIOLATED);
							break;
						case UNKNOWN:
						case NOT_APPLICABLE:
						case NOT_CHECKED:
						case IGNORED:
							CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_UNKNOWN);
							break;
						default:
							CssHandler.updateStyleClass(this, CssHandler.CSS_SEVERITY_NOMINAL);
							break;
					}
				} else {
					CssHandler.updateStyleClass(this, null);
					setText("");
					setGraphic(null);
				}
			}
		}));

		initialiseToolbarVisibility(displayTitledPane, toolbar, toggleShowToolbarItem);
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


	@FXML
	private void detachAttachItemAction(ActionEvent actionEvent) {
		if(DetachedTabUtil.isDetached((Stage) displayTitledPane.getScene().getWindow())) {
			DetachedTabUtil.attachTab((Stage) displayTitledPane.getScene().getWindow());
			informDisplayAttached();
		}
	}

	@Override
	protected void informDisplayAttached() {
		detachMenuItem.setDisable(true);
	}

	@Override
	protected void informDisplayDetached() {
		detachMenuItem.setDisable(false);
	}
}
