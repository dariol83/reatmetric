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

import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;

public class AboutDialogController implements Initializable {

    private static final Image LOGO_IMG = new Image(ConnectorStatusWidgetController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/logos/logo-full-color-640px.png"));

    @FXML
    public ImageView aboutImage;
    @FXML
    public javafx.scene.web.WebView aboutWebView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        aboutImage.setImage(LOGO_IMG);
        aboutWebView.getEngine().loadContent(buildAboutText(), "text/html");
    }

    private String buildAboutText() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><h2>ReatMetric UI " + ReatmetricUI.APPLICATION_VERSION).append("</h2>\n")
                .append("<p>Copyright (c) 2019-2023 Dario Lucia</p>")
                .append("<p>https://www.dariolucia.eu</p>")
                .append("<p>https://github.com/dariol83/reatmetric</p>")
                .append("<br /><p>Released under the Apache License 2.0</p>" +
                        "<br /><br /><p><i>Il semble que la perfection soit atteinte non quand il n'y a plus rien à ajouter, mais quand il n'y a plus rien à retrancher.</i></p>")
                .append("<p align=\"right\">Antoine de Saint-Exupéry</p></body></html>");
        return sb.toString();
    }
}
