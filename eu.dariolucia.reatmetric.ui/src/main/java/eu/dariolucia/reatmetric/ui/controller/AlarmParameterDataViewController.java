/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
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
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.ReferenceProperty;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class AlarmParameterDataViewController
		extends AbstractDataItemCurrentViewController<AlarmParameterData, AlarmParameterDataFilter>
		implements IAlarmParameterDataSubscriber {

	@FXML
	private TableColumn<ReferenceProperty<AlarmParameterData>, String> nameCol;
	@FXML
	private TableColumn<ReferenceProperty<AlarmParameterData>, String> currentValueCol;
	@FXML
	private TableColumn<ReferenceProperty<AlarmParameterData>, AlarmState> currentAlarmStateCol;
	@FXML
	private TableColumn<ReferenceProperty<AlarmParameterData>, Instant> genTimeCol;
	@FXML
	private TableColumn<ReferenceProperty<AlarmParameterData>, Instant> recTimeCol;
	@FXML
	private TableColumn<ReferenceProperty<AlarmParameterData>, String> lastNomValueCol;
	@FXML
	private TableColumn<ReferenceProperty<AlarmParameterData>, Instant> lastNomValueTimeCol;
	@FXML
	private TableColumn<ReferenceProperty<AlarmParameterData>, String> parentCol;

	@Override
	protected void doInitialize(URL url, ResourceBundle rb) {
		super.doInitialize(url, rb);
		this.nameCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getName()));
		this.currentValueCol.setCellValueFactory(
				o -> new ReadOnlyObjectWrapper<>(Objects.toString(o.getValue().get().getCurrentValue())));
		this.currentAlarmStateCol
				.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getCurrentAlarmState()));
		this.genTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getGenerationTime()));
		this.recTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getReceptionTime()));
		this.lastNomValueCol.setCellValueFactory(
				o -> new ReadOnlyObjectWrapper<>(Objects.toString(o.getValue().get().getLastNominalValue())));
		this.lastNomValueTimeCol
				.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getLastNominalValueTime()));
		this.parentCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().get().getPath().getParent().asString()));

		this.currentAlarmStateCol.setCellFactory(column -> {
			return new TableCell<ReferenceProperty<AlarmParameterData>, AlarmState>() {
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
						setStyle("-fx-font-weight: bold");
					} else {
						setText("");
						setGraphic(null);
					}
				}
			};
		});

		final ObservableList<ReferenceProperty<AlarmParameterData>> dataList = FXCollections.observableArrayList(
				new Callback<ReferenceProperty<AlarmParameterData>, Observable[]>() {
					@Override
					public Observable[] call(ReferenceProperty<AlarmParameterData> param) {
						return new Observable[]{
								param.referenceProperty()
						};
					}
				}
		);

		this.dataItemTableView.setItems(dataList);
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
				filter = new AlarmParameterDataFilter(null, null,null);
			}
			AlarmParameterDataFilter newFilter = new AlarmParameterDataFilter(entities.get(0).getPath(), null,
					filter.getAlarmStateList());
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
	protected boolean doCheckForItemAddition(AlarmParameterData apd) {
		return apd.getCurrentAlarmState() == AlarmState.ALARM || apd.getCurrentAlarmState() == AlarmState.ERROR || apd.getCurrentAlarmState() == AlarmState.WARNING;
	}

	@Override
	protected boolean doCheckForItemRemoval(AlarmParameterData apd) {
		return apd.getCurrentAlarmState() != AlarmState.ALARM && apd.getCurrentAlarmState() != AlarmState.ERROR && apd.getCurrentAlarmState() != AlarmState.WARNING;
	}

	@Override
	protected int doGetItemId(AlarmParameterData apd) {
		return apd.getExternalId();
	}

    //  private void updateApplicationAlarmStatus() {
    //		AlarmState currentState = AlarmState.NOMINAL;
    //		for(AlarmParameterData apd : this.dataItemTableView.getItems()) {
    //			if(apd.getCurrentAlarmState() == AlarmState.WARNING && (currentState != AlarmState.ALARM && currentState != AlarmState.ERROR)) {
    //				currentState = apd.getCurrentAlarmState();
    //			}
    //			if(apd.getCurrentAlarmState() == AlarmState.ALARM || apd.getCurrentAlarmState() == AlarmState.ERROR) {
    //				currentState = apd.getCurrentAlarmState();
    //			}
    //		}
    //		MonitoringCentreUI.setStatusIndicator(currentState);
    //	}

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
