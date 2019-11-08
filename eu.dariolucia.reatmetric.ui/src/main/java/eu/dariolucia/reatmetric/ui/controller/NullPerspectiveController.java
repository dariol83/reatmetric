/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.controller;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

/**
 * FXML Controller class
 *
 * @author dario
 */
public class NullPerspectiveController implements Initializable {

    @FXML
    private ImageView nullPerspectiveBgImage;
    
    @FXML
    private BorderPane nullPerspective;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.nullPerspectiveBgImage.fitWidthProperty().bind(this.nullPerspective.widthProperty());
        this.nullPerspectiveBgImage.fitHeightProperty().bind(this.nullPerspective.heightProperty());
    }    
    
}
