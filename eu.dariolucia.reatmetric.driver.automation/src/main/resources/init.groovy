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

def set(paramPath, value) {
    return _scriptManager.set(paramPath, value, false)
}

def raise(eventPath) {
    return _scriptManager.raise(eventPath, null, null)
}

def raise(eventPath, qualifier) {
    return _scriptManager.raise(eventPath, qualifier, null)
}

def raise(eventPath, qualifier, report) {
    return _scriptManager.raise(eventPath, qualifier, report)
}

def prepareActivity(activityPath) {
    return _scriptManager.prepareActivity(activityPath)
}

// -------

def connectorStatus(connectorName) {
    return _scriptManager.connectorStatus(connectorName)
}

def startConnector(connectorName) {
    return _scriptManager.startConnector(connectorName)
}

def stopConnector(connectorName) {
    return _scriptManager.stopConnector(connectorName)
}

def abortConnector(connectorName) {
    return _scriptManager.abortConnector(connectorName)
}

def initConnector(connectorName, keys, values) {
    return _scriptManager.initConnector(connectorName, keys, values)
}

def systemEntity(path) {
    return _scriptManager.systemEntity(path)
}

def enable(path) {
    return _scriptManager.enable(path)
}

def disable(path) {
    return _scriptManager.disable(path)
}