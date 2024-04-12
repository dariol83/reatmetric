/*
 * Copyright (c) 2023 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.ui;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.css.Styleable;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.File;

public class CssHandler {

    public static final String CSS_REATMETRIC_PREFIX = "reatmetric-";

    public static final String CSS_MODEL_STATUS_CELL_ENABLED = CSS_REATMETRIC_PREFIX + "model-status-cell-enabled";
    public static final String CSS_MODEL_STATUS_CELL_DISABLED = CSS_REATMETRIC_PREFIX + "model-status-cell-disabled";
    public static final String CSS_MODEL_STATUS_CELL_IGNORED = CSS_REATMETRIC_PREFIX + "model-status-cell-ignored";
    public static final String CSS_MODEL_STATUS_CELL_UNKNOWN = CSS_REATMETRIC_PREFIX + "model-status-cell-unknown";

    public static final String CSS_SEVERITY_ALARM = CSS_REATMETRIC_PREFIX + "severity-alarm";
    public static final String CSS_SEVERITY_ERROR = CSS_REATMETRIC_PREFIX + "severity-error";
    public static final String CSS_SEVERITY_WARNING = CSS_REATMETRIC_PREFIX + "severity-warning";
    public static final String CSS_SEVERITY_VIOLATED = CSS_REATMETRIC_PREFIX + "severity-violated";
    public static final String CSS_SEVERITY_NOMINAL = CSS_REATMETRIC_PREFIX + "severity-nominal";
    public static final String CSS_SEVERITY_UNKNOWN = CSS_REATMETRIC_PREFIX + "severity-unknown";

    public static final String CSS_VALIDITY_VALID = CSS_REATMETRIC_PREFIX + "validity-valid";
    public static final String CSS_VALIDITY_DISABLED = CSS_REATMETRIC_PREFIX + "validity-disabled";
    public static final String CSS_VALIDITY_ERROR = CSS_REATMETRIC_PREFIX + "validity-error";
    public static final String CSS_VALIDITY_INVALID = CSS_REATMETRIC_PREFIX + "validity-invalid";
    public static final String CSS_VALIDITY_UNKNOWN = CSS_REATMETRIC_PREFIX + "validity-unknown";

    public static final String CSS_ACTIVITY_STATE_RELEASE = CSS_REATMETRIC_PREFIX + "activity-state-release";
    public static final String CSS_ACTIVITY_STATE_TRANSMISSION = CSS_REATMETRIC_PREFIX + "activity-state-transmission";
    public static final String CSS_ACTIVITY_STATE_SCHEDULING = CSS_REATMETRIC_PREFIX + "activity-state-scheduling";
    public static final String CSS_ACTIVITY_STATE_EXECUTION = CSS_REATMETRIC_PREFIX + "activity-state-execution";
    public static final String CSS_ACTIVITY_STATE_VERIFICATION = CSS_REATMETRIC_PREFIX + "activity-state-verification";
    public static final String CSS_ACTIVITY_STATE_COMPLETED = CSS_REATMETRIC_PREFIX + "activity-state-completed";

    public static final String CSS_ACTIVITY_STATUS_OK = CSS_REATMETRIC_PREFIX + "activity-status-ok";
    public static final String CSS_ACTIVITY_STATUS_FAIL = CSS_REATMETRIC_PREFIX + "activity-status-fail";
    public static final String CSS_ACTIVITY_STATUS_FATAL = CSS_REATMETRIC_PREFIX + "activity-status-fatal";
    public static final String CSS_ACTIVITY_STATUS_PENDING = CSS_REATMETRIC_PREFIX + "activity-status-pending";
    public static final String CSS_ACTIVITY_STATUS_EXPECTED = CSS_REATMETRIC_PREFIX + "activity-status-expected";
    public static final String CSS_ACTIVITY_STATUS_UNKNOWN = CSS_REATMETRIC_PREFIX + "activity-status-unknown";
    public static final String CSS_ACTIVITY_STATUS_ERROR = CSS_REATMETRIC_PREFIX + "activity-status-error";
    public static final String CSS_ACTIVITY_STATUS_TIMEOUT = CSS_REATMETRIC_PREFIX + "activity-status-timeout";

    public static final String CSS_CONNECTOR_ALARM = CSS_REATMETRIC_PREFIX + "connector-alarm";
    public static final String CSS_CONNECTOR_NA = CSS_REATMETRIC_PREFIX + "connector-na";
    public static final String CSS_CONNECTOR_WARNING = CSS_REATMETRIC_PREFIX + "connector-warning";
    public static final String CSS_CONNECTOR_VIOLATED = CSS_REATMETRIC_PREFIX + "connector-violated";
    public static final String CSS_CONNECTOR_NOMINAL = CSS_REATMETRIC_PREFIX + "connector-nominal";

    public static final String CSS_CONNECTION_STATUS_ERROR = CSS_REATMETRIC_PREFIX + "connection-status-error";
    public static final String CSS_CONNECTION_STATUS_CONNECTING = CSS_REATMETRIC_PREFIX + "connection-status-connecting";
    public static final String CSS_CONNECTION_STATUS_NOMINAL = CSS_REATMETRIC_PREFIX + "connection-status-nominal";
    public static final String CSS_CONNECTION_STATUS_WARNING = CSS_REATMETRIC_PREFIX + "connection-status-warning";
    public static final String CSS_CONNECTION_STATUS_IDLE = CSS_REATMETRIC_PREFIX + "connection-status-idle";
    public static final String CSS_CONNECTION_STATUS_NOT_INIT = CSS_REATMETRIC_PREFIX + "connection-status-not-init";


    public static final String CSS_PARAMETER_RAW_SEPARATOR = CSS_REATMETRIC_PREFIX + "parameter-raw-separator";

    private static final SimpleStringProperty CSS;

    public static final String REATMETRIC_CSS_DEFAULT_THEME_KEY = "reatmetric.css.default.theme";

    static {
        String defaultCss = CssHandler.class.getResource("/eu/dariolucia/reatmetric/ui/fxml/css/reatmetric_clear.css").toExternalForm();
        CSS = new SimpleStringProperty(defaultCss);
        String externalCss = System.getProperty(REATMETRIC_CSS_DEFAULT_THEME_KEY);
        // Use the external styling if the property below is detected
        if(externalCss != null) {
            // Apply external CSS
            CSS.set(new File(externalCss).toPath().toUri().toASCIIString());
        }
    }

    private CssHandler() {
        throw new IllegalCallerException("Not to be called");
    }

    public static ReadOnlyStringProperty CSSProperty() {
        return CSS;
    }

    public static String getCSSProperty() {
        return CSS.get();
    }

    public static void setCSSProperty(String newCss) {
        CSS.set(newCss);
    }

    public static void applyTo(Parent n, boolean reset) {
        if(getCSSProperty() == null) {
            return;
        }
        if(reset) {
            n.getStylesheets().removeIf(o -> true);
        }
        n.getStylesheets().add(getCSSProperty());
    }

    public static void applyTo(Parent n) {
        applyTo(n, true);
    }

    public static void applyTo(Scene scene) {
        if(getCSSProperty() == null) {
            return;
        }
        scene.getStylesheets().removeIf(o -> true);
        if(getCSSProperty() != null) {
            scene.getStylesheets().add(getCSSProperty());
        }
    }

    public static void updateStyleClass(Styleable node, String styleClass) {
        updateStyleClass(node, styleClass, false);
    }

    public static void updateStyleClass(Styleable node, String styleClass, boolean reset) {
        if(reset) {
            node.getStyleClass().clear();
        } else {
            node.getStyleClass().removeIf(sc -> sc.startsWith(CssHandler.CSS_REATMETRIC_PREFIX));
        }
        if(styleClass != null) {
            node.getStyleClass().add(0, styleClass);
        }
    }
}
