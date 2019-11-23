package eu.dariolucia.reatmetric.processing.impl.processors;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.*;

public class SystemEntityBuilder {

    final private int id;

    final private SystemEntityPath path;

    private Status status;

    private AlarmState alarmState = AlarmState.UNKNOWN;

    private boolean changedSinceLastBuild;

    final private SystemEntityType type;

    public SystemEntityBuilder(int id, SystemEntityPath path, SystemEntityType type) {
        this.id = id;
        this.path = path;
        this.type = type;
        this.changedSinceLastBuild = false;
    }

    public boolean isChangedSinceLastBuild() {
        return changedSinceLastBuild;
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

    public SystemEntity build(IUniqueId updateId) {
        SystemEntity se = new SystemEntity(updateId, id, path, status, alarmState, type);
        this.changedSinceLastBuild = false;
        return se;
    }
}
