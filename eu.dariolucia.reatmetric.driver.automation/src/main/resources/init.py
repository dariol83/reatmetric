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

def wait_for_event(eventPath, timeoutSeconds):
    return _scriptManager.wait_for_event(eventPath, timeoutSeconds)

def wait_for_parameter(parameter, timeoutSeconds):
    return _scriptManager.wait_for_event(parameter, timeoutSeconds)

def inject_parameter(parampath, value):
    return _scriptManager.inject_parameter(parampath, value, False)

def raise_event(eventpath):
    return _scriptManager.raise_event(eventpath, None, None)

def raise_event(eventpath, qualifier):
    return _scriptManager.raise_event(eventpath, qualifier, None)

def raise_event(eventpath, qualifier, report):
    return _scriptManager.raise_event(eventpath, qualifier, report)

def prepare_activity(activitypath):
    return _scriptManager.prepare_activity(activitypath)

def connector_status(connectorname):
    return _scriptManager.connector_status(connectorname)

def delete_scheduled_activity(externalId):
    return _scriptManager.delete_scheduled_activity(externalId)

def start_connector(connectorname):
    return _scriptManager.start_connector(connectorname)

def stop_connector(connectorname):
    return _scriptManager.stop_connector(connectorname)

def abort_connector(connectorname):
    return _scriptManager.abort_connector(connectorname)

def init_connector(connectorname, keys, values):
    return _scriptManager.init_connector(connectorname, keys, values)

def system_entity(path):
    return _scriptManager.system_entity(path)

def enable(path):
    return _scriptManager.enable(path)

def disable(path):
    return _scriptManager.disable(path)

def ignore(path):
    return _scriptManager.ignore(path)