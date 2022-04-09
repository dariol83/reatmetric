def info(message) {
    _scriptManager.info(message)
}

def warning(message) {
    _scriptManager.warning(message)
}

def alarm(message) {
    _scriptManager.alarm(message)
}

def parameter(paramPath) {
    return _scriptManager.parameter(paramPath)
}

def event(eventPath) {
    return _scriptManager.event(eventPath)
}

def wait_for_event(eventPath, timeoutSeconds) {
    return _scriptManager.wait_for_event(eventPath, timeoutSeconds)
}

def wait_for_parameter(parameterPath, timeoutSeconds) {
    return _scriptManager.wait_for_parameter(parameterPath, timeoutSeconds)
}

def inject_parameter(paramPath, value) {
    return _scriptManager.inject_parameter(paramPath, value, false)
}

def raise_event(eventPath) {
    return _scriptManager.raise_event(eventPath, null, null)
}

def raise_event(eventPath, qualifier) {
    return _scriptManager.raise_event(eventPath, qualifier, null)
}

def raise_event(eventPath, qualifier, report) {
    return _scriptManager.raise_event(eventPath, qualifier, report)
}

def prepare_activity(activityPath) {
    return _scriptManager.prepare_activity(activityPath)
}

def delete_scheduled_activity(externalId) {
    return _scriptManager.delete_scheduled_activity(externalId)
}

// -------

def connector_status(connectorName) {
    return _scriptManager.connector_status(connectorName)
}

def start_connector(connectorName) {
    return _scriptManager.start_connector(connectorName)
}

def stop_connector(connectorName) {
    return _scriptManager.stop_connector(connectorName)
}

def abort_connector(connectorName) {
    return _scriptManager.abort_connector(connectorName)
}

def init_connector(connectorName, keys, values) {
    return _scriptManager.init_connector(connectorName, keys, values)
}

// -------

def system_entity(path) {
    return _scriptManager.system_entity(path)
}

def enable(path) {
    return _scriptManager.enable(path)
}

def disable(path) {
    return _scriptManager.disable(path)
}

def ignore(path) {
    return _scriptManager.ignore(path)
}