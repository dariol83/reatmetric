package eu.dariolucia.reatmetric.api.processing.scripting;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.parameters.Validity;

public interface IParameterBinding extends IEntityBinding {

    Object rawValue();

    Object value();

    AlarmState alarmState();

    boolean inAlarm();

    boolean valid();

    Validity validity();

    Long containerId();

    String route();

}
