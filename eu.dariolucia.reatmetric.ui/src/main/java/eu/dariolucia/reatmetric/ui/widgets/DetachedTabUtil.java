package eu.dariolucia.reatmetric.ui.widgets;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Static class that helps detaching tabs into stand-alone stages and re-attaching stages to original tab panes.
 */
public class DetachedTabUtil {

    private static final Map<Stage, StageInformation> STAGE2INFO = new HashMap<>();

    public static Stage findDetachedTabById(String id) {
        for(Map.Entry<Stage, StageInformation> e : STAGE2INFO.entrySet()) {
            if(Objects.equals(e.getValue().getOriginalTab().getId(), id)) {
                return e.getKey();
            }
        }
        return null;
    }

    public static boolean isDetached(Stage stage) {
        return STAGE2INFO.containsKey(stage);
    }

    public static Stage detachTab(Tab t, URL stylesheetUrl, Image stageImage) {
        TabPane tabPane = t.getTabPane();
        Parent tabContents = (Parent) t.getContent();
        // Create a detached scene parent
        Stage stage = new Stage();
        t.setContent(null);
        // Save the action to invoke, when a tab closes
        final EventHandler<Event> closeHandler = t.getOnCloseRequest();
        t.setOnCloseRequest(null);
        tabPane.getTabs().remove(t);
        Scene scene = new Scene(tabContents, tabContents.getLayoutBounds().getWidth(), tabContents.getLayoutBounds().getHeight());
        if(stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        }
        stage.setScene(scene);
        stage.setTitle(t.getText());
        stage.setUserData(t.getUserData());
        if(stageImage != null) {
            stage.getIcons().add(stageImage);
        }
        // When closing the stage, the dispose() method of the controller is called
        stage.setOnCloseRequest(ev -> {
            // contentsController.dispose();
            if(closeHandler != null) {
                closeHandler.handle(ev);
            }
            stage.close();
            STAGE2INFO.remove(stage);
        });

        STAGE2INFO.put(stage, new StageInformation(tabPane, t, closeHandler));
        stage.show();

        return stage;
    }

    public static Tab attachTab(Stage externalStage) {
        StageInformation si = STAGE2INFO.get(externalStage);
        if(si == null) {
            throw new IllegalStateException("No stage information found for stage " + externalStage);
        }
        TabPane tabPane = si.getOriginalTabPane();
        Parent stageContents = externalStage.getScene().getRoot();
        // If the tabPane is not visible, then I cannot reattach
        if(!tabPane.isVisible()) {
            return null;
        }
        // Fetch old tab
        Tab t = si.getOriginalTab();
        // t.setClosable(true);
        t.setContent(stageContents);
        // t.setText(title);
        t.setOnCloseRequest(si.getOriginalCloseHandler());
        t.setUserData(externalStage.getUserData());
        tabPane.getTabs().add(t);

        externalStage.setOnCloseRequest(null);
        externalStage.close();
        // Remove the stage
        STAGE2INFO.remove(externalStage);

        return t;
    }

    private static class StageInformation {
        private final TabPane originalTabPane;
        private final Tab originalTab;
        private final EventHandler<Event> originalCloseHandler;

        public StageInformation(TabPane originalTabPane, Tab originalTab, EventHandler<Event> originalCloseHandler) {
            this.originalTabPane = originalTabPane;
            this.originalTab = originalTab;
            this.originalCloseHandler = originalCloseHandler;
        }

        public TabPane getOriginalTabPane() {
            return originalTabPane;
        }

        public EventHandler<Event> getOriginalCloseHandler() {
            return originalCloseHandler;
        }

        public Tab getOriginalTab() {
            return originalTab;
        }
    }
}
