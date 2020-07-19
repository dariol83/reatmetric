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

function set(paramPath, value) {
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