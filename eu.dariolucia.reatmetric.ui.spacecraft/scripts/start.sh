#!/bin/bash

# TODO: Remove javafx.dnd.deserializeObjectGraph after replacing serialized object drag-and-drop payloads and their consumers.
java --module-path="bin" -Djavafx.dnd.deserializeObjectGraph=true -Dreatmetric.core.config=<PATH TO CORE CONFIGURATION XML> --add-exports javafx.base/com.sun.javafx.event=org.controlsfx.controls -m eu.dariolucia.reatmetric.ui/eu.dariolucia.reatmetric.ui.ReatmetricUI
