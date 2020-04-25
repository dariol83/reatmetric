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

import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.udd.*;
import eu.dariolucia.reatmetric.ui.utils.OrderedProperties;
import eu.dariolucia.reatmetric.ui.utils.UserDisplayCoordinator;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * FXML Controller class
 *
 *
 * @author dario
 */
public class UserDisplayTabWidgetController extends AbstractDisplayController implements Initializable {

	// Live/retrieval controls
	@FXML
	protected ToggleButton liveTgl;
	@FXML
	protected Button goToStartBtn;
	@FXML
	protected Button goBackFastBtn;
	@FXML
	protected Button goBackOneBtn;
	@FXML
	protected Button goToEndBtn;
	@FXML
	protected Button goForwardFastBtn;
	@FXML
	protected Button goForwardOneBtn;
	@FXML
	protected Button selectTimeBtn;

	@FXML
	protected ToggleButton liveTimeTgl;

	@FXML
	protected VBox parentVBox;

	@FXML
	protected VBox innerBox;

	// Progress indicator for data retrieval
	@FXML
	protected ProgressIndicator progressIndicator;

	// Popup selector for date/time
	protected final Popup dateTimePopup = new Popup();

	// Time selector controller
	protected DateTimePickerWidgetController dateTimePickerController;

	//
	protected final List<AbstractChartManager> charts = new CopyOnWriteArrayList<>();

	protected volatile ParameterDataFilter currentParameterFilter = new ParameterDataFilter(null, new LinkedList<>(),null,null,null, null);

	protected volatile EventDataFilter currentEventFilter = new EventDataFilter(null, new LinkedList<>(),null, null,null,null, null);

	protected final int timeWindowSize = 60000;

	protected final int timeUnit = 5000;

	private volatile Instant currentMin = null;
	private volatile Instant currentMax = null;

	private volatile Instant lastDataMaxGenerationTime; // Lastest generation time of data

	private Timer timer = null;
	private volatile boolean liveTimeTracker = true;
	private volatile boolean live = false;
	private UserDisplayCoordinator controller;

	private HBox lineBox = null;
	private Stage independentStage;

	@Override
	public void doInitialize(URL url, ResourceBundle rb) {
		if (this.liveTgl != null) {
			this.goToStartBtn.disableProperty().bind(this.liveTgl.selectedProperty());
			this.goBackFastBtn.disableProperty().bind(this.liveTgl.selectedProperty());
			this.goBackOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
			this.goToEndBtn.disableProperty().bind(this.liveTgl.selectedProperty());
			this.goForwardFastBtn.disableProperty().bind(this.liveTgl.selectedProperty());
			this.goForwardOneBtn.disableProperty().bind(this.liveTgl.selectedProperty());
			this.selectTimeBtn.disableProperty().bind(this.liveTgl.selectedProperty());
		}

		this.dateTimePopup.setAutoHide(true);
		this.dateTimePopup.setHideOnEscape(true);

		try {
			URL datePickerUrl = getClass().getClassLoader()
					.getResource("eu/dariolucia/reatmetric/ui/fxml/DateTimePickerWidget.fxml");
			FXMLLoader loader = new FXMLLoader(datePickerUrl);
			Parent dateTimePicker = loader.load();
			this.dateTimePickerController = loader.getController();
			this.dateTimePopup.getContent().addAll(dateTimePicker);
			// Load the controller hide with select
			this.dateTimePickerController.setActionAfterSelection(() -> {
				this.dateTimePopup.hide();
				Instant theTime = this.dateTimePickerController.getSelectedTime();
				fetchRecords(theTime.minusMillis(getTimeWindowSize()), theTime, true);
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.live = true;
		// Register to the coordinator
		this.controller = UserDisplayCoordinator.instance();
		this.controller.register(this);
	}

	@FXML
	protected void addLineChartMenuItemSelected(ActionEvent e) {
		addLineChart();
	}

	@FXML
	protected void addAreaChartMenuItemSelected(ActionEvent e) {
		addAreaChart();
	}

	@FXML
	protected void addBarChartMenuItemSelected(ActionEvent e) {
		addBarChart();
	}

	@FXML
	protected void addScatterChartMenuItemSelected(ActionEvent e) {
		addScatterChart();
	}

	private void fetchRecords(final Instant minTime, final Instant maxTime, final boolean clear) {
		final ParameterDataFilter pdf = getCurrentParameterFilter();
		final EventDataFilter edf = getCurrentEventFilter();
		// Retrieve the next one and add it on top
		markProgressBusy();
		ReatmetricUI.threadPool(getClass()).execute(() -> {
			try {
				List<AbstractDataItem> messages = new LinkedList<>();

				// Retrieve the parameters
				List<ParameterData> pmessages = ReatmetricUI.selectedSystem().getSystem()
						.getParameterDataMonitorService().retrieve(minTime, 100, RetrievalDirection.TO_FUTURE, pdf);
				messages.addAll(pmessages);
				// Repeat until endTime is reached
				if (pmessages.size() > 0 && pmessages.get(pmessages.size() - 1).getGenerationTime().isBefore(maxTime)) {
					List<ParameterData> newMessages = ReatmetricUI.selectedSystem().getSystem()
							.getParameterDataMonitorService()
							.retrieve(pmessages.get(messages.size() - 1), 100, RetrievalDirection.TO_FUTURE, pdf);
					messages.addAll(newMessages);
				}

				// Retrieve the events
				List<EventData> emessages = ReatmetricUI.selectedSystem().getSystem()
						.getEventDataMonitorService().retrieve(minTime, 100, RetrievalDirection.TO_FUTURE, edf);
				messages.addAll(emessages);
				// Repeat until endTime is reached
				if (emessages.size() > 0 && emessages.get(emessages.size() - 1).getGenerationTime().isBefore(maxTime)) {
					List<EventData> newMessages = ReatmetricUI.selectedSystem().getSystem()
							.getEventDataMonitorService()
							.retrieve(emessages.get(emessages.size() - 1), 100, RetrievalDirection.TO_FUTURE, edf);
					messages.addAll(newMessages);
				}
				messages.removeIf(eventData -> eventData.getGenerationTime().isAfter(maxTime));

				setData(minTime, maxTime, messages, clear);
			} catch (Exception e) {
				e.printStackTrace();
			}
			markProgressReady();
		});
	}

	protected void setData(Instant minTime, Instant maxTime, List<AbstractDataItem> messages, boolean clear) {
		// As this data comes from the archive, set the last data max time
		lastDataMaxGenerationTime = lastDataMaxGenerationTime == null ? maxTime : (lastDataMaxGenerationTime.isBefore(maxTime) ? maxTime : lastDataMaxGenerationTime);
		Platform.runLater(() -> {
			if (clear) {
				clearCharts();
			}
			this.currentMin = minTime;
			this.currentMax = maxTime;
			this.charts.forEach(a -> a.setBoundaries(minTime, maxTime));
			this.charts.forEach(a -> a.plot(messages));
			this.selectTimeBtn.setText(formatTime(maxTime));
		});
	}

	public void updateDataItems(List<? extends AbstractDataItem> items) {
		if (this.live) {
			// As this data comes from the live stream, set the last data max time
			Optional<Instant> maxGenerationTime = items.stream().map(AbstractDataItem::getGenerationTime).max((Comparator.naturalOrder()));
			if(maxGenerationTime.isPresent()) {
				lastDataMaxGenerationTime = lastDataMaxGenerationTime == null ? maxGenerationTime.get() : (lastDataMaxGenerationTime.isBefore(maxGenerationTime.get()) ? maxGenerationTime.get() : lastDataMaxGenerationTime);
			}
			this.charts.forEach(a -> a.plot((List<AbstractDataItem>)items));
		}
	}

	@FXML
	protected void liveToggleSelected(ActionEvent e) {
		if (this.liveTgl.isSelected()) {
			Instant now = Instant.now();
			fetchRecords(now.minusMillis(getTimeWindowSize()), now, true);
			startSubscription();
		} else {
			stopSubscription();
		}
	}

	@FXML
	protected void liveTimeToggleSelected(ActionEvent e) {
		this.liveTimeTracker = this.liveTimeTgl.isSelected();
	}

	private void clearCharts() {
		this.charts.forEach(AbstractChartManager::clear);
	}

	private int getTimeWindowSize() {
		return this.timeWindowSize;
	}

	public void setIndependentStage(Stage independentStage) {
		this.independentStage = independentStage;
	}

	@FXML
	protected void goToStart(ActionEvent e) {
		if (!isProgressBusy()) {
			moveToTimeBeginning();
		}
	}

	private void moveToTimeEnd() {
		final ParameterDataFilter pdf = getCurrentParameterFilter();
		final EventDataFilter edf = getCurrentEventFilter();
		final int timeWindow = getTimeWindowSize();
		// Retrieve the next one and add it on top
		markProgressBusy();
		ReatmetricUI.threadPool(getClass()).execute(() -> {
			try {
				List<AbstractDataItem> messages = new LinkedList<>();

				// Retrieve the parameters
				List<ParameterData> pmessages = ReatmetricUI.selectedSystem().getSystem()
						.getParameterDataMonitorService().retrieve(Instant.ofEpochSecond(3600 * 24 * 365 * 1000L), 100, RetrievalDirection.TO_PAST, pdf);
				messages.addAll(pmessages);
				// Repeat until endTime is reached
				Instant minTime = null;
				if (pmessages.size() > 0) {
					minTime = pmessages.get(0).getGenerationTime().minusMillis(timeWindow);

					if (pmessages.get(pmessages.size() - 1).getGenerationTime().isAfter(minTime)) {
						List<ParameterData> newMessages = ReatmetricUI.selectedSystem().getSystem()
								.getParameterDataMonitorService()
								.retrieve(pmessages.get(pmessages.size() - 1), 100, RetrievalDirection.TO_PAST, pdf);
						messages.addAll(newMessages);
					}
				}
				// Retrieve the events
				List<EventData> emessages = ReatmetricUI.selectedSystem().getSystem()
						.getEventDataMonitorService().retrieve(Instant.ofEpochSecond(3600 * 24 * 365 * 1000L), 100, RetrievalDirection.TO_PAST, edf);
				messages.addAll(emessages);
				// Repeat until endTime is reached
				if (emessages.size() > 0) {
					if(minTime == null) {
						minTime = emessages.get(0).getGenerationTime().minusMillis(timeWindow);
					} else {
						minTime = emessages.get(0).getGenerationTime().minusMillis(timeWindow).isBefore(minTime) ? emessages.get(0).getGenerationTime().minusMillis(timeWindow) : minTime;
					}

					if (emessages.get(emessages.size() - 1).getGenerationTime().isAfter(minTime)) {
						List<EventData> newMessages = ReatmetricUI.selectedSystem().getSystem()
								.getEventDataMonitorService()
								.retrieve(emessages.get(emessages.size() - 1), 100, RetrievalDirection.TO_PAST, edf);
						messages.addAll(newMessages);
					}
				}
				// Remove too old items
				if(minTime != null) {
					for (Iterator<AbstractDataItem> it = messages.iterator(); it.hasNext(); ) {
						if (it.next().getGenerationTime().isBefore(minTime)) {
							it.remove();
						}
					}
				}
				setData(minTime, messages.get(0).getGenerationTime(), messages, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			markProgressReady();
		});
	}

	private void moveToTimeBeginning() {
		final ParameterDataFilter pdf = getCurrentParameterFilter();
		final EventDataFilter edf = getCurrentEventFilter();
		final int timeWindow = getTimeWindowSize();
		// Retrieve the next one and add it on top
		markProgressBusy();
		ReatmetricUI.threadPool(getClass()).execute(() -> {
			try {
				List<AbstractDataItem> messages = new LinkedList<>();

				List<ParameterData> pmessages = ReatmetricUI.selectedSystem().getSystem()
						.getParameterDataMonitorService().retrieve(Instant.EPOCH, 100, RetrievalDirection.TO_FUTURE, pdf);
				messages.addAll(pmessages);

				// Repeat until endTime is reached
				Instant maxTime = null;
				if (pmessages.size() > 0) {
					maxTime = pmessages.get(0).getGenerationTime().plusMillis(timeWindow);

					if (pmessages.get(pmessages.size() - 1).getGenerationTime().isBefore(maxTime)) {
						List<ParameterData> newMessages = ReatmetricUI.selectedSystem().getSystem()
								.getParameterDataMonitorService()
								.retrieve(pmessages.get(pmessages.size() - 1), 100, RetrievalDirection.TO_FUTURE, pdf);
						messages.addAll(newMessages);
					}
				}
				// Retrieve the events
				List<EventData> emessages = ReatmetricUI.selectedSystem().getSystem()
						.getEventDataMonitorService().retrieve(Instant.EPOCH, 100, RetrievalDirection.TO_FUTURE, edf);
				messages.addAll(emessages);
				// Repeat until endTime is reached
				if (emessages.size() > 0) {
					if(maxTime == null) {
						maxTime = emessages.get(0).getGenerationTime().plusMillis(timeWindow);
					} else {
						maxTime = emessages.get(0).getGenerationTime().plusMillis(timeWindow).isAfter(maxTime) ? emessages.get(0).getGenerationTime().plusMillis(timeWindow) : maxTime;
					}

					if (emessages.get(emessages.size() - 1).getGenerationTime().isBefore(maxTime)) {
						List<EventData> newMessages = ReatmetricUI.selectedSystem().getSystem()
								.getEventDataMonitorService()
								.retrieve(emessages.get(emessages.size() - 1), 100, RetrievalDirection.TO_FUTURE, edf);
						messages.addAll(newMessages);
					}
				}

				// Remove too late items
				if(maxTime != null) {
					for (Iterator<AbstractDataItem> it = messages.iterator(); it.hasNext(); ) {
						if (it.next().getGenerationTime().isAfter(maxTime)) {
							it.remove();
						}
					}
				}
				setData(messages.get(0).getGenerationTime(), maxTime, messages, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			markProgressReady();
		});
	}

	@FXML
	protected void goBackFast(ActionEvent e) {
		if (!isProgressBusy()) {
			fetchRecords(getCurrentMinTime().minusMillis(getTimeWindowSize()), getCurrentMinTime(), false);
		}
	}

	private Instant getCurrentMinTime() {
		return this.currentMin;
	}

	@FXML
	protected void goBackOne(ActionEvent e) {
		if (!isProgressBusy()) {
			fetchRecords(getCurrentMinTime().minusMillis(getTimeUnit()), getCurrentMaxTime().minusMillis(getTimeUnit()),
					false);
		}
	}

	private Instant getCurrentMaxTime() {
		return this.currentMax;
	}

	private int getTimeUnit() {
		return this.timeUnit;
	}

	@FXML
	protected void goToEnd(ActionEvent e) {
		if (!isProgressBusy()) {
			moveToTimeEnd();
		}
	}

	@FXML
	protected void goForwardFast(ActionEvent e) {
		if (!isProgressBusy()) {
			fetchRecords(getCurrentMaxTime(), getCurrentMaxTime().plusMillis(getTimeWindowSize()), false);
		}
	}

	@FXML
	protected void goForwardOne(ActionEvent e) {
		if (!isProgressBusy()) {
			fetchRecords(getCurrentMinTime().plusMillis(getTimeUnit()), getCurrentMaxTime().plusMillis(getTimeUnit()),
					false);
		}
	}

	@FXML
	protected void selectTimeButtonSelected(ActionEvent e) {
		if (this.dateTimePopup.isShowing()) {
			this.dateTimePopup.hide();
		} else {
			Bounds b = this.selectTimeBtn.localToScreen(this.selectTimeBtn.getBoundsInLocal());
			this.dateTimePopup.setX(b.getMinX());
			this.dateTimePopup.setY(b.getMaxY());
			this.dateTimePopup.getScene().getRoot().getStylesheets().add(getClass().getClassLoader()
					.getResource("eu/dariolucia/reatmetric/ui/fxml/css/MainView.css").toExternalForm());
			this.dateTimePopup.show(this.innerBox.getScene().getWindow());
		}
	}

	public boolean isLive() {
		return this.live;
	}

	@Override
	public void dispose() {
		super.dispose();
		this.controller.deregister(this);
	}

	@Override
	public void doSystemDisconnected(IReatmetricSystem s, boolean oldStatus) {
		if (this.liveTgl != null) {
			this.liveTgl.setSelected(false);
		}
		this.innerBox.getParent().getParent().setDisable(true);
		stopSubscription();
		clearCharts();
		// If you are detached, close the stage
		Parent parentNode = parentVBox.getParent();
		if(independentStage != null) {
			independentStage.setOnCloseRequest(null);
			independentStage.close();
		}
	}

	@Override
	public void doSystemConnected(IReatmetricSystem s, boolean oldStatus) {
		// Start subscription if there
		if (this.liveTgl == null || this.liveTgl.isSelected()) {
			clearCharts();
			startSubscription();
		}
		if (this.liveTgl != null) {
			this.liveTgl.setSelected(true);
		}
		this.innerBox.getParent().getParent().setDisable(false);
	}

	private void markProgressBusy() {
		this.progressIndicator.setVisible(true);
	}

	private void markProgressReady() {
		Platform.runLater(() -> {
			this.progressIndicator.setVisible(false);
		});
	}

	private boolean isProgressBusy() {
		return this.progressIndicator.isVisible();
	}

	protected final void startSubscription() {
		this.live = true;
		this.charts.forEach(a -> a.switchToLive(true));
		if (this.currentMin == null) {
			this.currentMax = getMaxTimeReference();
			this.currentMin = this.currentMax.minusMillis(getTimeWindowSize());
			this.charts.forEach(a -> a.setBoundaries(this.currentMin, this.currentMax));
		}
		if (this.timer != null) {
			this.timer.cancel();
		}
		this.timer = new Timer(true);
		this.timer.schedule(new TimerTask() {
			@Override
			public void run() {
				Platform.runLater(() -> {
					currentMax = getMaxTimeReference();
					currentMin = currentMax.minusMillis(getTimeWindowSize());
					charts.forEach(a -> a.setBoundaries(currentMin, currentMax));
					selectTimeBtn.setText(formatTime(currentMax.minusMillis(getTimeUnit())));
				});
			}
		}, getTimeUnit(), getTimeUnit());
	}

	private Instant getMaxTimeReference() {
		if(this.liveTimeTracker) {
			return Instant.now().plusMillis(getTimeUnit());
		} else {
			// last retrieve data generation time
			Instant lastRetriever = lastDataMaxGenerationTime;
			if(lastRetriever != null) {
				return lastRetriever.plusMillis(getTimeUnit());
			} else {
				return Instant.now().plusMillis(getTimeUnit());
			}
		}
	}

	public ParameterDataFilter getCurrentParameterFilter() {
		return this.currentParameterFilter;
	}

	public EventDataFilter getCurrentEventFilter() {
		return this.currentEventFilter;
	}

	protected final void stopSubscription() {
		if (this.timer != null) {
			this.timer.cancel();
		}
		this.timer = null;
		this.charts.forEach(a -> a.switchToLive(false));

		this.live = false;
	}

	@Override
	protected Window retrieveWindow() {
		return this.liveTgl.getScene().getWindow();
	}

	@Override
	protected Node doBuildNodeForPrinting() {
		WritableImage image = innerBox.snapshot(null, null);
		ImageView v = new ImageView(image);
		v.setFitWidth(innerBox.getWidth());
		v.setFitHeight(innerBox.getHeight());
		return v;
	}

	private AbstractChartManager addLineChart() {
		LineChart<Instant, Number> l = new LineChart<>(new InstantAxis(), new NumberAxis());
		return initialiseTimeChart(l);
	}

	private AbstractChartManager addAreaChart() {
		AreaChart<Instant, Number> l = new AreaChart<>(new InstantAxis(), new NumberAxis());
		return initialiseTimeChart(l);
	}

	private AbstractChartManager addBarChart() {
		BarChart<String, Number> l = new BarChart<>(new CategoryAxis(), new NumberAxis());
		return initialiseBarChart(l);
	}

	private AbstractChartManager addScatterChart() {
		ScatterChart<Instant, Number> l = new ScatterChart<>(new InstantAxis(), new NumberAxis());
		return initialiseScatterChart(l);
	}

	private AbstractChartManager initialiseBarChart(BarChart<String, Number> l) {
		l.setAnimated(false);
		l.getXAxis().setTickLabelsVisible(true);

		// Add to list
		AbstractChartManager udd = new XYBarChartManager((o, arg) -> updateFilter(), l);
		// l.setPrefHeight(200);
		addToPane(l);
		// l.prefWidthProperty().bind(this.innerBox.widthProperty());
		// this.innerBox.getChildren().add(l);
		this.charts.add(udd);
		this.innerBox.getParent().layout();
		return udd;
	}

	private void addToPane(XYChart<?, ?> l) {
		if (this.lineBox == null) {
			this.lineBox = new HBox();
			this.lineBox.setPrefHeight(300);
			this.lineBox.setMinHeight(300);
			this.lineBox.setPadding(new Insets(10, 10, 10, 10));
			this.lineBox.setSpacing(10);
			this.lineBox.prefWidthProperty().bind(this.innerBox.widthProperty());
			this.innerBox.getChildren().add(this.lineBox);
			this.lineBox.getChildren().add(l);
			HBox.setHgrow(l, Priority.ALWAYS);
		} else {
			this.lineBox.getChildren().add(l);
			HBox.setHgrow(l, Priority.ALWAYS);
			this.lineBox.layout();
			this.lineBox = null;
		}
	}

	private AbstractChartManager initialiseTimeChart(XYChart<Instant, Number> l) {
		l.setAnimated(false);
		l.getXAxis().setTickLabelsVisible(true);

		l.getXAxis().setAutoRanging(false);
		((InstantAxis) l.getXAxis()).setLowerBound(Instant.now().minusMillis(getTimeWindowSize() - getTimeUnit()));
		((InstantAxis) l.getXAxis()).setUpperBound(Instant.now().plusMillis(getTimeUnit()));

		// Add to list
		XYTimeChartManager udd = new XYTimeChartManager((o, arg) -> updateFilter(), l);

		addToPane(l);

		this.charts.add(udd);
		this.innerBox.getParent().layout();
		return udd;
	}

	private AbstractChartManager initialiseScatterChart(ScatterChart<Instant, Number> l) {
		l.setAnimated(false);
		l.getXAxis().setTickLabelsVisible(true);

		l.getXAxis().setAutoRanging(false);
		((InstantAxis) l.getXAxis()).setLowerBound(Instant.now().minusMillis(getTimeWindowSize() - getTimeUnit()));
		((InstantAxis) l.getXAxis()).setUpperBound(Instant.now().plusMillis(getTimeUnit()));

		((NumberAxis) l.getYAxis()).setTickUnit(1.0);
		((NumberAxis) l.getYAxis()).setTickLabelsVisible(false);

		// Add to list
		XYScatterChartManager udd = new XYScatterChartManager((o, arg) -> updateFilter(), l);

		addToPane(l);

		this.charts.add(udd);
		this.innerBox.getParent().layout();
		return udd;
	}

	protected void updateFilter() {
		Set<SystemEntityPath> selectedParameters = new LinkedHashSet<>();
		Set<SystemEntityPath> selectedEvents = new LinkedHashSet<>();
		for (AbstractChartManager acm : this.charts) {
			selectedParameters.addAll(acm.getPlottedParameters());
			selectedEvents.addAll(acm.getPlottedEvents());
		}
		this.currentParameterFilter = new ParameterDataFilter(null, new LinkedList<>(selectedParameters),null,null,null, null);
		this.currentEventFilter = new EventDataFilter(null, new LinkedList<>(selectedEvents),null,null,null, null, null);
		// Update the subscriptions
		this.controller.filterUpdated();
	}

	public Properties getChartDescription() {
		// Iterate on the chart managers
		Properties props = new OrderedProperties();
		int incrementalId = 0;
		for(AbstractChartManager acm : charts) {
			String chartType = acm.getChartType();
			List<String> items = acm.getCurrentEntityPaths();
			// Add the type as incremental key (3 digits, padded) plus type, and value is the list of paths, split by comma
			props.put(String.format("%03d", incrementalId) + "." + chartType, items.stream().reduce("", (a,b) -> a.isEmpty() ? b : a + "," + b));
			++incrementalId;
		}
		return props;
	}

	public void loadPreset(Properties p) {
		Set<String> chartSets = p.keySet().stream().map(Object::toString).collect(Collectors.toCollection(TreeSet::new));
		for(String chartSet : chartSets) {
			String chartType = chartSet.substring(4);
			AbstractChartManager chartManager = null;
			List<String> items = Arrays.asList(((String) p.get(chartSet)).split(","));
			switch(chartType) {
				case "line":
				{
					chartManager = addLineChart();
				}
				break;
				case "area":
				{
					chartManager = addAreaChart();
				}
				break;
				case "bar":
				{
					chartManager = addBarChart();
				}
				break;
				case "scatter":
				{
					chartManager = addScatterChart();
				}
				break;
			}
			if(chartManager != null) {
				chartManager.addItems(items);
			}
		}
	}
}
