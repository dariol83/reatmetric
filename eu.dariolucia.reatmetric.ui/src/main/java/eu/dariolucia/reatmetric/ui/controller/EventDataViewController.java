/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.controller;

import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.IEventDataSubscriber;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats;
import javafx.beans.property.ReadOnlyObjectWrapper;
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
public class EventDataViewController extends AbstractDataItemLogViewController<EventData, EventDataFilter>
		implements IEventDataSubscriber {

	@FXML
	private TableColumn<EventData, String> nameCol;
	@FXML
	private TableColumn<EventData, String> typeCol;
	@FXML
	private TableColumn<EventData, String> routeCol;
	@FXML
	private TableColumn<EventData, Severity> severityCol;
	@FXML
	private TableColumn<EventData, Instant> genTimeCol;
	@FXML
	private TableColumn<EventData, Instant> recTimeCol;
	@FXML
	private TableColumn<EventData, String> sourceCol;
	@FXML
	private TableColumn<EventData, String> qualifierCol;
	@FXML
	private TableColumn<EventData, String> parentCol;

	@Override
	protected void doInitialize(URL url, ResourceBundle rb) {
		super.doInitialize(url, rb);
		this.nameCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getName()));
		this.typeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getType()));
		this.routeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getRoute()));
		this.severityCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getSeverity()));
		this.genTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getGenerationTime()));
		this.recTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getReceptionTime()));
		this.sourceCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getSource()));
		this.qualifierCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getQualifier()));
		this.parentCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getPath().getParent().asString()));

		this.severityCol.setCellFactory(column -> {
			return new TableCell<EventData, Severity>() {
				@Override
				protected void updateItem(Severity item, boolean empty) {
					super.updateItem(item, empty);
					if (item != null && !empty && !isEmpty()) {
						setText(item.name());
						switch (item) {
						case ALARM:
							setTextFill(Color.DARKRED);
							setStyle("-fx-font-weight: bold");
							break;
						case WARN:
							setTextFill(Color.CHOCOLATE);
							setStyle("-fx-font-weight: bold");
							break;
						default:
							setTextFill(Color.DARKGREEN);
							setStyle("-fx-font-weight: bold");
							break;
						}
					} else {
						setText("");
						setGraphic(null);
					}
				}
			};
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
			EventDataFilter filter = getCurrentFilter();
			if (filter == null) {
				filter = new EventDataFilter(null, null, null, null, null);
			}
			EventDataFilter newFilter = new EventDataFilter(entities.get(0).getPath(), filter.getRouteList(),
					filter.getTypeList(), filter.getSourceList(), filter.getSeverityList());
			applyFilter(newFilter);
			success = true;
		}

		event.setDropCompleted(success);

		event.consume();
	}

	@Override
	public void dataItemsReceived(List<EventData> messages) {
		informDataItemsReceived(messages);
	}

	@Override
	protected void doServiceSubscribe(EventDataFilter selectedFilter) throws ReatmetricException {
		ReatmetricUI.selectedSystem().getSystem().getEventDataMonitorService().subscribe(this, selectedFilter);
	}

	@Override
	protected void doServiceUnsubscribe() throws ReatmetricException {
		ReatmetricUI.selectedSystem().getSystem().getEventDataMonitorService().unsubscribe(this);
	}

	@Override
	protected List<EventData> doRetrieve(EventData om, int n, RetrievalDirection direction, EventDataFilter filter)
			throws ReatmetricException {
		return ReatmetricUI.selectedSystem().getSystem().getEventDataMonitorService().retrieve(om, n, direction,
				filter);
	}

	@Override
	protected List<EventData> doRetrieve(Instant selectedTime, int n, RetrievalDirection direction,
			EventDataFilter filter) throws ReatmetricException {
		return ReatmetricUI.selectedSystem().getSystem().getEventDataMonitorService().retrieve(selectedTime, n,
				direction, filter);
	}

	@Override
	protected Instant doGetGenerationTime(EventData om) {
		return om.getGenerationTime();
	}

	@Override
	protected URL doGetFilterWidget() {
		return getClass().getClassLoader()
				.getResource("eu/dariolucia/reatmetric/ui/fxml/EventDataFilterWidget.fxml");
	}

	@Override
	protected String doGetComponentId() {
		return "EventDataView";
	}

}
