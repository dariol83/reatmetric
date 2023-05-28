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


package eu.dariolucia.reatmetric.ui.udd;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class AbstractChartManager<T, K> {

	private static File SAVE_IMAGE_INITIAL_DIR = null;

	private final Consumer<AbstractChartManager<T, K>> changeListener;
	private final Set<SystemEntityPath> systemEntities = new TreeSet<>();
	private final Map<SystemEntityPath, XYChart.Series<T, K>> object2series = new LinkedHashMap<>();
	private final Map<String, Boolean> serie2visibility = new TreeMap<>();

	protected volatile boolean live = true;
	protected volatile Instant maxGenerationTimeOnChart = Instant.EPOCH;

	public AbstractChartManager(Consumer<AbstractChartManager<T, K>> informer) {
		changeListener = informer;
	}

	protected void addSerie(SystemEntityPath item, XYChart.Series<T, K> serie) {
		this.object2series.put(item, serie);
	}

	protected XYChart.Series<T, K> getSerie(SystemEntityPath item) {
		return this.object2series.get(item);
	}

	protected boolean containsSerie(SystemEntityPath item) {
		return this.object2series.containsKey(item);
	}

	protected XYChart.Series<T, K> removeSerie(SystemEntityPath item) {
		return this.object2series.remove(item);
	}

	public Set<SystemEntityPath> getPlottedSystemEntities() {
		return this.systemEntities;
	}

	protected void addPlottedSystemEntities(SystemEntityPath path) {
		this.systemEntities.add(path);
		notifyObservers();
	}

	protected void notifyObservers() {
		if(changeListener != null) {
			changeListener.accept(this);
		}
	}

	protected void addMenu(XYChart<T, K> chart) {
		chart.setOnMouseClicked(event -> {
			if (MouseButton.SECONDARY.equals(event.getButton())) {
				buildAndShowMenu(chart, event);
			}
		});
	}

	protected boolean isSerieVisible(String name) {
		Boolean visible = serie2visibility.get(name);
		return visible != null && visible;
	}

	protected void setSerieVisible(String name, boolean value) {
		serie2visibility.put(name, value);
	}

	protected void removeSerieVisibility(String name) {
		serie2visibility.remove(name);
	}

	private void buildAndShowMenu(XYChart<T, K> chart, MouseEvent originEvent) {
		final Menu visibilityItem = new Menu("Visibility");
		for(XYChart.Series<T, K> serie : chart.getData()) {
			final XYChart.Series<T, K> fserie = serie;
			final CheckMenuItem shownItem = new CheckMenuItem(fserie.getName());
			shownItem.setSelected(isSerieVisible(fserie.getName()));
			shownItem.setOnAction(event -> {
				boolean newVisibility = !isSerieVisible(fserie.getName());
				setSerieVisible(fserie.getName(), newVisibility);

				applySerieVisibility(fserie, newVisibility);
			});
			visibilityItem.getItems().add(shownItem);
		}

		final Menu deleteSeriesItem = new Menu("Delete");
		for(XYChart.Series<T, K> serie : chart.getData()) {
			final XYChart.Series<T, K> fserie = serie;
			final MenuItem deleteSerieItem = new MenuItem(fserie.getName());
			deleteSerieItem.setOnAction(event -> {
				deleteSerieFrom(chart, fserie);
			});
			deleteSeriesItem.getItems().add(deleteSerieItem);
		}

		/*
		final MenuItem deleteItem = new MenuItem("Delete chart");
		deleteItem.setOnAction(event -> {
			ObservableList<XYChart.Series<T, K>> data = chart.getData();
			for(XYChart.Series<T, K> ser : data) {
				String param = ser.getName();
				parameter2chartSeries.get(param).remove(ser);
			}
			vboxChartContainer.getChildren().remove(chart);
		});
		*/

		final MenuItem copyItem = new MenuItem("Copy to clipboard");
		copyItem.setOnAction(event -> {
			WritableImage image = new WritableImage((int) chart.getWidth(), (int) chart.getHeight());
			chart.setMinSize(chart.getWidth(), chart.getHeight());
			image = chart.snapshot(null, image);
			chart.setMinSize(-1, -1);
			Clipboard clipboard = Clipboard.getSystemClipboard();
			ClipboardContent content = new ClipboardContent();
			content.putImage(image);
			clipboard.setContent(content);
		});

		final MenuItem exportItem = new MenuItem("Export as image file");
		exportItem.setOnAction(event -> {
			// Open dialog
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Save as image");
			fileChooser.getExtensionFilters().addAll(
					new FileChooser.ExtensionFilter("PNG file", "*.png")
			);
			if(SAVE_IMAGE_INITIAL_DIR != null) {
				fileChooser.setInitialDirectory(SAVE_IMAGE_INITIAL_DIR);
			}

			File toSave = fileChooser.showSaveDialog(chart.getScene().getWindow());
			if(toSave != null) {
				SAVE_IMAGE_INITIAL_DIR = toSave.getParentFile();
				try {
					if (toSave.exists()) {
						toSave.delete();
					}
					toSave.createNewFile();
					// Prepare the image
					WritableImage image = new WritableImage((int) chart.getWidth(), (int) chart.getHeight());
					chart.setMinSize(chart.getWidth(), chart.getHeight());
					image = chart.snapshot(null, image);
					chart.setMinSize(-1, -1);

					// Format and export
					ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", toSave);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		final MenuItem exportCsvItem = new MenuItem("Export as CSV file");
		exportCsvItem.setOnAction(event -> {
			// Open dialog
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Save as CSV");
			fileChooser.getExtensionFilters().addAll(
					new FileChooser.ExtensionFilter("CSV file", "*.csv")
			);
			if(SAVE_IMAGE_INITIAL_DIR != null) {
				fileChooser.setInitialDirectory(SAVE_IMAGE_INITIAL_DIR);
			}

			File toSave = fileChooser.showSaveDialog(chart.getScene().getWindow());
			if(toSave != null) {
				SAVE_IMAGE_INITIAL_DIR = toSave.getParentFile();
				try {
					if (toSave.exists()) {
						toSave.delete();
					}
					toSave.createNewFile();
					//
					PrintStream ps = new PrintStream(new FileOutputStream(toSave));
					for(XYChart.Series<T, K> series : chart.getData()) {
						if(series.getNode().isVisible()) {
							exportTo(ps, series);
						}
					}
					ps.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		final ContextMenu menu = new ContextMenu(visibilityItem, deleteSeriesItem, new SeparatorMenuItem(), /* deleteItem, */ new SeparatorMenuItem(), exportItem, exportCsvItem, copyItem);
		menu.show(chart.getScene().getWindow(), originEvent.getScreenX(), originEvent.getScreenY());
	}

	private void deleteSerieFrom(XYChart<T, K> chart, XYChart.Series<T, K> fserie) {
		chart.getData().remove(fserie);
		SystemEntityPath toRemove = null;
		for (Map.Entry<SystemEntityPath, XYChart.Series<T, K>> pEntry : object2series.entrySet()) {
			if(pEntry.getValue().equals(fserie)) {
				toRemove = pEntry.getKey();
				break;
			}
		}
		if(toRemove != null) {
			object2series.remove(toRemove);
			removeSerieVisibility(fserie.getName());
		}
		chart.getData().remove(fserie);
		notifyObservers();
	}

	protected void applySerieVisibility(XYChart.Series<T, K> fserie, boolean newVisibility) {
		if(fserie.getNode() != null) {
			fserie.getNode().setVisible(newVisibility);
		}
		for(XYChart.Data<T, K> point : fserie.getData()) {
			if(point.getNode() != null) {
				point.getNode().setVisible(newVisibility);
			}
		}
	}

	private void exportTo(PrintStream ps, XYChart.Series<T, K> series) {
		for(XYChart.Data<T, K> d : series.getData()) {
			ps.printf("%s, %s, %s%n", series.getName(), d.getXValue(), d.getYValue());
		}
	}

	public void switchToLive(boolean b) {
		this.live = b;
	}

	public abstract void setBoundaries(Instant min, Instant max);
	
	public abstract void plot(List<AbstractDataItem> data);

	public void clear() {
		this.object2series.values().forEach(a -> a.getData().clear());
		this.maxGenerationTimeOnChart = Instant.EPOCH;
	}

	public abstract String getChartType();

	public List<String> getCurrentEntityPaths() {
		return object2series.keySet().stream().map(SystemEntityPath::asString).collect(Collectors.toList());
	}
    public abstract void addItems(List<String> items);

	public abstract SystemEntityType getSystemElementType();

	public Instant getLatestReceivedGenerationTime() {
		return maxGenerationTimeOnChart;
	}
}
