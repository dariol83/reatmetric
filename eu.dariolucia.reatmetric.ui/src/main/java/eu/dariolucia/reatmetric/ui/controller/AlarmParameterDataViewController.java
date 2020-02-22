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
import eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats;
import javafx.application.Platform;
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
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * FIXME: wrong assumption with InternaId (can change!), use ExternalId, to be fixed
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
				o -> new ReadOnlyObjectWrapper<>(Objects.toString(o.getValue().getCurrentValue())));
		this.currentAlarmStateCol
				.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getCurrentAlarmState()));
		this.genTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getGenerationTime()));
		this.recTimeCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getReceptionTime()));
		this.lastNomValueCol.setCellValueFactory(
				o -> new ReadOnlyObjectWrapper<>(Objects.toString(o.getValue().getLastNominalValue())));
		this.lastNomValueTimeCol
				.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getLastNominalValueTime()));
		this.parentCol.setCellValueFactory(o -> new ReadOnlyObjectWrapper<>(o.getValue().getPath().getParent().asString()));

		this.currentAlarmStateCol.setCellFactory(column -> {
			return new TableCell<AlarmParameterData, AlarmState>() {
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
    protected void addDataItems(List<AlarmParameterData> messages, boolean fromLive, boolean addOnTop) {
		if(this.liveTgl.isSelected()) {
	        Platform.runLater(() -> {
	            if (!this.displayTitledPane.isDisabled() && (!fromLive || (fromLive && (this.liveTgl == null || this.liveTgl.isSelected())))) {
	            	for(AlarmParameterData apd : messages) {
	            		int idx = findIndexOf(apd);
	            		if(idx > -1) {
	            			if(apd.getCurrentAlarmState() != AlarmState.ALARM && apd.getCurrentAlarmState() != AlarmState.ERROR && apd.getCurrentAlarmState() != AlarmState.WARNING) {
	                			// If in the table, and the current alarm state is not ERROR, ALARM or WARNING, remove it from the table
	            				this.dataItemTableView.getItems().remove(idx);
	            			} else {
	            				// If an alarm parameter is already in the table, then update the value in place (replace the item)
	            				this.dataItemTableView.getItems().set(idx, apd); // TODO: this is SLOW LIKE HELL!
	            			}
	            		} else {
	            			if(apd.getCurrentAlarmState() == AlarmState.ALARM || apd.getCurrentAlarmState() == AlarmState.ERROR || apd.getCurrentAlarmState() == AlarmState.WARNING) {
	            				// If not in the table, add it to top if addOnTop is true, add it to bottom if addOnTop is false
	            				if(addOnTop) {
	            					this.dataItemTableView.getItems().add(0, apd);
	            				} else {
	            					this.dataItemTableView.getItems().add(apd);
	            				}
	            			}
	            		}
		            	// If not in the table, and the current alarm state is not ERROR, ALARM or WARNING, ignore
	            	}
	                if (!fromLive) {
	                    this.dataItemTableView.scrollTo(0);
	                }
	                this.dataItemTableView.refresh();
	                updateSelectTime();
	                // updateApplicationAlarmStatus();
	            }
	        });
		} else {
			super.addDataItems(messages, fromLive, addOnTop);
		}
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

	private int findIndexOf(AlarmParameterData apd) {
		// XXX: hash index recommended
		for(int i = 0; i < this.dataItemTableView.getItems().size(); ++i) {
			AlarmParameterData existingData = this.dataItemTableView.getItems().get(i);
			// Compare by path
			if(existingData.getPath().equals(apd.getPath())) {
				return i;
			}
		}
		return -1;
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
