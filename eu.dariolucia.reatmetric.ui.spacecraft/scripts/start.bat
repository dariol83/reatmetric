@echo off

java --module-path="bin" -Dreatmetric.core.config=<PATH TO CORE CONFIGURATION XML> --add-exports javafx.base/com.sun.javafx.event=org.controlsfx.controls -m eu.dariolucia.reatmetric.ui/eu.dariolucia.reatmetric.ui.ReatmetricUI

pause