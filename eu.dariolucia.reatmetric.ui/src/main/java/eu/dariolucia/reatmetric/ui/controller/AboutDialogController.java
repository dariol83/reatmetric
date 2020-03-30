/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
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

    private static final Image LOGO_IMG = new Image(ConnectorStatusWidgetController.class.getResourceAsStream("/eu/dariolucia/reatmetric/ui/fxml/images/logo.png"));

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
                .append("<p>Copyright (c) 2019 Dario Lucia</p>")
                .append("<p>https://www.dariolucia.eu</p>")
                .append("<p>https://github.com/dariol83/reatmetric</p>")
                .append("<br /><p>Released under the Apache License 2.0</p>" +
                        "<br /><br /><p><i>Perfection is achieved not when there is nothing more to add, but when there is nothing left to take away.</i></p>")
                .append("<p align=\"right\">Le Petit Prince</p></body></html>");
        return sb.toString();
    }
}
