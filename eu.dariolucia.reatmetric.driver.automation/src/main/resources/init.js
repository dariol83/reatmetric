function info(var message) {
    _scriptManager.info(message);
}

function warning(var message) {
    _scriptManager.warning(message);
}

function alarm(var message) {
    _scriptManager.alarm(message);
}

function parameter(var paramPath) {
    return _scriptManager.parameter(paramPath);
}

function event(var eventPath) {
    return _scriptManager.event(eventPath);
}

function injectRaw(var paramPath, var value) {
    return _scriptManager.inject(paramPath, value, true);
}

function injectEng(var paramPath, var value) {
    return _scriptManager.inject(paramPath, value, false);
}

function raise(var eventPath) {
    return _scriptManager.raise(eventPath);
}

function prepareActivity(var activityPath) {
    return _scriptManager.prepareActivity(activityPath);
}