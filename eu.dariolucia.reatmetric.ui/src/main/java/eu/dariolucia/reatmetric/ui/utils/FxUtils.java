/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.ui.utils;

import eu.dariolucia.reatmetric.ui.controller.ConnectorStatusWidgetController;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class FxUtils {

    public static void setMenuItemImage(MenuItem menuItem, String imageLocation) {
        Image img = new Image(FxUtils.class.getResourceAsStream(imageLocation));
        ImageView menuIcon = new ImageView(img);
        menuItem.setGraphic(menuIcon);
    }
}
