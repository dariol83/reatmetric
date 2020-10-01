function info(message) {
    _scriptManager.info(message);
}

function warning(message) {
    _scriptManager.warning(message);
}

function alarm(message) {
    _scriptManager.alarm(message);
}

function parameter(paramPath) {
    return _scriptManager.parameter(paramPath);
}

function event(eventPath) {
    return _scriptManager.event(eventPath);
}

function inject(paramPath, value) {
    return _scriptManager.set(paramPath, value, false);
}

function raise(eventPath) {
    return _scriptManager.raise(eventPath, null, null);
}

function raise(eventPath, qualifier) {
    return _scriptManager.raise(eventPath, qualifier, null);
}

function raise(eventPath, qualifier, report) {
    return _scriptManager.raise(eventPath, qualifier, report);
}

function prepareActivity(activityPath) {
    return _scriptManager.prepareActivity(activityPath);
}

// -------

function connectorStatus(connectorName) {
    return _scriptManager.connectorStatus(connectorName);
}

function startConnector(connectorName) {
    return _scriptManager.startConnector(connectorName);
}

function stopConnector(connectorName) {
    return _scriptManager.stopConnector(connectorName);
}

function abortConnector(connectorName) {
    return _scriptManager.abortConnector(connectorName);
}

function initConnector(connectorName, keys, values) {
    return _scriptManager.initConnector(connectorName, keys, values);
}

function systemEntity(path) {
    return _scriptManager.systemEntity(path);
}

function enable(path) {
    return _scriptManager.enable(path);
}

function disable(path) {
    return _scriptManager.disable(path);
}