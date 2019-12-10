package eu.dariolucia.reatmetric.processing.definition.scripting;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.parameters.Validity;

public interface IParameterBinding extends IEntityBinding {

    Object sourceValue();

    Object value();

    AlarmState alarmState();

    boolean inAlarm();

    boolean valid();

    Validity validity();

    Long containerId();

    String route();

}
