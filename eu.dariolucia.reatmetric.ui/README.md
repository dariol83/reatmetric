### How to setup a working UI for testing
To be written
### Known issues
#### I cannot schedule activities. When I right-click on an activity and I select 'Schedule', nothing happens and I have an IllegalAccessError exception.
The UI application must be started with the following VM option:

    --add-exports=javafx.base/com.sun.javafx.event=org.controlsfx.controls

This is a known problem, see https://github.com/controlsfx/controlsfx/wiki/Using-ControlsFX-with-JDK-9-and-above