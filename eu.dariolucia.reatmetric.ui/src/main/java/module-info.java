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

open module eu.dariolucia.reatmetric.ui {

    requires java.logging;
    requires java.rmi;
    requires java.desktop;
    requires jdk.jsobject;

    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.web;
    requires javafx.media;
    requires javafx.swing;

    requires org.controlsfx.controls;
    requires eu.dariolucia.jfx.timeline;

    requires eu.dariolucia.reatmetric.api;


    requires json.path;

    exports eu.dariolucia.reatmetric.ui;

    uses eu.dariolucia.reatmetric.api.IReatmetricRegister;
}