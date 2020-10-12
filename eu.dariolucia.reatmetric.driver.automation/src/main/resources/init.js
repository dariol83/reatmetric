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

function inject_parameter(paramPath, value) {
    return _scriptManager.inject_parameter(paramPath, value, false);
}

function raise_event(eventPath) {
    return _scriptManager.raise_event(eventPath, null, null);
}

function raise_event(eventPath, qualifier) {
    return _scriptManager.raise_event(eventPath, qualifier, null);
}

function raise_event(eventPath, qualifier, report) {
    return _scriptManager.raise_event(eventPath, qualifier, report);
}

function prepare_activity(activityPath) {
    return _scriptManager.prepare_activity(activityPath);
}

// -------

function connector_status(connectorName) {
    return _scriptManager.connector_status(connectorName);
}

function start_connector(connectorName) {
    return _scriptManager.start_connector(connectorName);
}

function stop_connector(connectorName) {
    return _scriptManager.stop_connector(connectorName);
}

function abort_connector(connectorName) {
    return _scriptManager.abort_connector(connectorName);
}

function init_connector(connectorName, keys, values) {
    return _scriptManager.init_connector(connectorName, keys, values);
}

function system_entity(path) {
    return _scriptManager.system_entity(path);
}

function enable(path) {
    return _scriptManager.enable(path);
}

function disable(path) {
    return _scriptManager.disable(path);
}

function ignore(path) {
    return _scriptManager.ignore(path);
}