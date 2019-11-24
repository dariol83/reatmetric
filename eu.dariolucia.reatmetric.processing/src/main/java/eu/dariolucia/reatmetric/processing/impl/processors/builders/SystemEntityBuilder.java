package eu.dariolucia.reatmetric.processing.impl.processors.builders;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.*;

/**
 * This helper class is used to build a {@link SystemEntity} instance.
 */
public class SystemEntityBuilder extends AbstractDataItemBuilder<SystemEntity> {

    private Status status;

    private AlarmState alarmState = AlarmState.UNKNOWN;

    private final SystemEntityType type;

    public SystemEntityBuilder(int id, SystemEntityPath path, SystemEntityType type) {
        super(id, path);
        this.type = type;
    }

    public void setStatus(Status status) {
        if(status != this.status) {
            this.status = status;
            this.changedSinceLastBuild = true;
        }
    }

    public void setAlarmState(AlarmState alarmState) {
        if(this.alarmState != alarmState) {
            this.alarmState = alarmState;
            this.changedSinceLastBuild = true;
        }
    }

    @Override
    public SystemEntity build(IUniqueId updateId) {
        SystemEntity se = new SystemEntity(updateId, id, path, status, alarmState, type);
        this.changedSinceLastBuild = false;
        return se;
    }
}
