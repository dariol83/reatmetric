package eu.dariolucia.reatmetric.ui.widgets;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.WindowEvent;

public class EnhancedTitledPane extends TitledPane {

    private static final Image IMG = new Image(EnhancedTitledPane.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/16px/cog.svg.png"));

    private final Label titleLabel = new Label("");
    private final ToggleButton titleButton = new ToggleButton();
    private final SimpleObjectProperty<ContextMenu> titleContextMenu = new SimpleObjectProperty<>();

    public EnhancedTitledPane() {
        super();
        initialiseButton();
    }

    public EnhancedTitledPane(String s, Node node) {
        super(s, node);
        initialiseButton();
    }

    private void initialiseButton() {
        titleContextMenu.addListener(this::contextMenuUpdated);
        titleButton.setGraphic(new ImageView(IMG));
        titleButton.setMinWidth(IMG.getWidth());
        titleButton.setMaxWidth(IMG.getWidth());
        titleButton.setPrefWidth(IMG.getWidth());
        titleButton.setMinHeight(IMG.getHeight());
        titleButton.setMaxHeight(IMG.getHeight());
        titleButton.setPrefHeight(IMG.getHeight());
        titleButton.visibleProperty().bind(titleContextMenuProperty().isNotNull());
        titleButton.setOnAction(this::menuButtonClicked);
        titleLabel.textProperty().bind(super.textProperty());
        BorderPane bPane = new BorderPane();
        bPane.setLeft(titleLabel);
        Label label = new Label(" ");
        bPane.setCenter(label);
        bPane.setRight(titleButton);
        bPane.prefWidthProperty().bind(super.widthProperty().subtract(IMG.getWidth()/2 + 4));
        this.setGraphic(bPane);
    }

    private void menuButtonClicked(ActionEvent actionEvent) {
        if(titleButton.isSelected() && titleContextMenuProperty().get() != null)  {
            double menuWidth = titleContextMenuProperty().get().getWidth();
            titleContextMenuProperty().get().show(this.titleButton, Side.BOTTOM, 0, 1);
        } else if(!titleButton.isSelected() && titleContextMenuProperty().get() != null) {
            titleContextMenuProperty().get().hide();
        }
    }

    private void contextMenuUpdated(ObservableValue<? extends ContextMenu> observableValue, ContextMenu oldMenu, ContextMenu currentMenu) {
        if(oldMenu != null) {
            oldMenu.setOnHidden(null);
        }
        if(currentMenu != null) {
            currentMenu.setOnHidden(this::deselectOnMenuHiding);
        }
    }

    private void deselectOnMenuHiding(WindowEvent windowEvent) {
        this.titleButton.setSelected(false);
    }

    public SimpleObjectProperty<ContextMenu> titleContextMenuProperty() {
        return titleContextMenu;
    }

    public void setTitleContextMenu(ContextMenu titleContextMenu) {
        titleContextMenuProperty().set(titleContextMenu);
    }

    public ContextMenu getTitleContextMenu() {
        return titleContextMenuProperty().get();
    }
}
