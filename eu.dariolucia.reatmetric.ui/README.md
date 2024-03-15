### How to setup a working UI for testing
To be written
### How to customise the UI appearance with CSS
The UI application must be started with the following VM option:

    -Dreatmetric.css.default.theme=<path to CSS file>

Two examples of such file (which can be copied and modified) can be found inside the reatmetric.ui module, 
src/main/resources/eu/dariolucia/reatmetric/ui/fxml/css. If no property is specified, the UI application will start 
with the reatmetric_clear.css theme by default, which in turns uses the standard OpenJFX CSS file Modena.css. The latest
version of such file can be found by following this link:
https://github.com/openjdk/jfx/blob/master/modules/javafx.controls/src/main/resources/com/sun/javafx/scene/control/skin/modena/modena.css

The file contains a lot of explanations. To make a customisation, it is enough to overwrite the desired property in the
custom CSS file. Example: the reatmetric_dark style overwrites the property -fx-base of the class .root. This property
is used by the Modena style to derive all the gradients of background automatically.

The reference guide for OpenJFX styling is provided here:
https://openjfx.io/javadoc/19/javafx.graphics/javafx/scene/doc-files/cssref.html

### Known issues
#### I cannot schedule activities. When I right-click on an activity and I select 'Schedule', nothing happens and I have an IllegalAccessError exception.
The UI application must be started with the following VM option:

    --add-exports=javafx.base/com.sun.javafx.event=org.controlsfx.controls

This is a known problem, see https://github.com/controlsfx/controlsfx/wiki/Using-ControlsFX-with-JDK-9-and-above