def info(message):
    _scriptManager.info(message)


def warning(message):
    _scriptManager.warning(message)

def alarm(message):
    _scriptManager.alarm(message)

def parameter(parampath):
    return _scriptManager.parameter(parampath)

def event(eventpath):
    return _scriptManager.event(eventpath)

def set(parampath, value):
    return _scriptManager.set(parampath, value, False)

def raise_event(eventpath):
    return _scriptManager.raiseEvent(eventpath, None, None)

def raise_event(eventpath, qualifier):
    return _scriptManager.raiseEvent(eventpath, qualifier, None)

def raise_event(eventpath, qualifier, report):
    return _scriptManager.raiseEvent(eventpath, qualifier, report)

def prepare_activity(activitypath):
    return _scriptManager.prepareActivity(activitypath)

def connector_status(connectorname):
    return _scriptManager.connectorStatus(connectorname)

def start_connector(connectorname):
    return _scriptManager.startConnector(connectorname)

def stop_connector(connectorname):
    return _scriptManager.stopConnector(connectorname)

def abort_connector(connectorname):
    return _scriptManager.abortConnector(connectorname)

def init_connector(connectorname, keys, values):
    return _scriptManager.initConnector(connectorname, keys, values)

def system_entity(path):
    return _scriptManager.systemEntity(path)

def enable(path):
    return _scriptManager.enable(path)

def disable(path):
    return _scriptManager.disable(path)