const RTMT_PARAMETERS_PATH = "parameters";
const RTMT_PARAMETER_CURRENT_STATE_PATH = "current";
const RTMT_PARAMETER_STREAM_PATH = "stream";
const RTMT_EVENTS_PATH = "events";
const RTMT_MESSAGES_PATH = "messages";

const RTMT_REGISTRATION_URL = "register";
const RTMT_GET_URL = "get";
const RTMT_DEREGISTRATION_URL = "deregister";
const RTMT_LIST_URL = "list";

class ReatMetric {
    constructor(host, port, name) {
        this.host = host;
        this.port = port;
        this.name = name;
    }

    /*********************************************************
     * Parameters
     *********************************************************/

    async listParameters() {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_PARAMETERS_PATH + "/" + RTMT_LIST_URL;
        const response = await fetch(toFetch, {
            method: 'GET', // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            headers: {
              'Content-Type': 'application/json'
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: null // body data type must match "Content-Type" header
        });
        const data = await response.json();
        return data;
    }

    parameterFilter(parentPath, parameterPathList, routeList, validityList, alarmStateList, externalIdList) {
        return new ParameterFilter(parentPath, parameterPathList, routeList, validityList, alarmStateList, externalIdList);
    }

    /*********************************************************
     * Parameter State
     *********************************************************/

    async registerToStateParameters(filter) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_PARAMETERS_PATH + "/" + RTMT_PARAMETER_CURRENT_STATE_PATH + "/" + RTMT_REGISTRATION_URL;
        const response = await fetch(toFetch, {
            method: 'POST', // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            headers: {
              'Content-Type': 'application/json'
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: JSON.stringify(filter) // body data type must match "Content-Type" header
        });
        const data = await response.json();
        return data.key;
    }

    async deregisterFromStateParameters(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_PARAMETERS_PATH  + "/" + RTMT_PARAMETER_CURRENT_STATE_PATH + "/" + RTMT_DEREGISTRATION_URL + "/" + key;
        const response = await fetch(toFetch, {
            method: 'DELETE', // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            headers: {
              'Content-Type': 'application/json'
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: null // body data type must match "Content-Type" header
        });
    }

    async getStateParameters(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_PARAMETERS_PATH + "/" + RTMT_PARAMETER_CURRENT_STATE_PATH  + "/" + RTMT_GET_URL + "/" + key;
        const response = await fetch(toFetch, {
            method: 'GET', // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            headers: {
              'Content-Type': 'application/json'
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: null // body data type must match "Content-Type" header
        });
        if (response.status === 200) {
            const data = await response.json();
            return data;
        } else {
            return null;
        }
    }

    /*********************************************************
     * Parameter Stream
     *********************************************************/

    async registerToStreamParameters(filter) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_PARAMETERS_PATH + "/" + RTMT_PARAMETER_STREAM_PATH + "/" + RTMT_REGISTRATION_URL;
        const response = await fetch(toFetch, {
            method: 'POST', // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            headers: {
              'Content-Type': 'application/json'
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: JSON.stringify(filter) // body data type must match "Content-Type" header
        });
        const data = await response.json();
        return data.key;
    }

    async deregisterFromStreamParameters(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_PARAMETERS_PATH  + "/" + RTMT_PARAMETER_STREAM_PATH + "/" + RTMT_DEREGISTRATION_URL + "/" + key;
        const response = await fetch(toFetch, {
            method: 'DELETE', // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            headers: {
              'Content-Type': 'application/json'
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: null // body data type must match "Content-Type" header
        });
    }

    async getStreamParameters(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_PARAMETERS_PATH + "/" + RTMT_PARAMETER_STREAM_PATH  + "/" + RTMT_GET_URL + "/" + key;
        const response = await fetch(toFetch, {
            method: 'GET', // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            headers: {
              'Content-Type': 'application/json'
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: null // body data type must match "Content-Type" header
        });
        if (response.status === 200) {
            const data = await response.json();
            return data;
        } else {
            return null;
        }
    }

    /*********************************************************
     * Events
     *********************************************************/

    async listEvents() {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_EVENTS_PATH + "/" + RTMT_LIST_URL;
        const response = await fetch(toFetch, {
            method: 'GET', // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            headers: {
              'Content-Type': 'application/json'
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: null // body data type must match "Content-Type" header
        });
        const data = await response.json();
        return data;
    }

    async registerToEvents(filter) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_EVENTS_PATH + "/" + RTMT_REGISTRATION_URL;
        const response = await fetch(toFetch, {
            method: 'POST', // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            headers: {
              'Content-Type': 'application/json'
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: JSON.stringify(filter) // body data type must match "Content-Type" header
        });
        const data = await response.json();
        return data.key;
    }

    async deregisterFromEvents(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_EVENTS_PATH + "/" + RTMT_DEREGISTRATION_URL + "/" + key;
        const response = await fetch(toFetch, {
            method: 'DELETE', // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            headers: {
              'Content-Type': 'application/json'
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: null // body data type must match "Content-Type" header
        });
    }

    async getEvents(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_EVENTS_PATH + "/" + RTMT_GET_URL + "/" + key;
        const response = await fetch(toFetch, {
            method: 'GET', // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            headers: {
              'Content-Type': 'application/json'
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: null // body data type must match "Content-Type" header
        });
        if (response.status === 200) {
            const data = await response.json();
            return data;
        } else {
            return null;
        }
    }

    eventFilter(parentPath, eventPathList, sourceList, routeList, typeList, severityList, externalIdList) {
        return new EventFilter(parentPath, eventPathList, sourceList, routeList, typeList, severityList, externalIdList);
    }

    /*********************************************************
     * Operational Messages
     *********************************************************/

    async registerToMessages(filter) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_MESSAGES_PATH + "/" + RTMT_REGISTRATION_URL;
        const response = await fetch(toFetch, {
            method: 'POST', // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            headers: {
              'Content-Type': 'application/json'
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: JSON.stringify(filter) // body data type must match "Content-Type" header
        });
        const data = await response.json();
        return data.key;
    }

    async deregisterFromMessages(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_MESSAGES_PATH + "/" + RTMT_DEREGISTRATION_URL + "/" + key;
        const response = await fetch(toFetch, {
            method: 'DELETE', // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            headers: {
              'Content-Type': 'application/json'
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: null // body data type must match "Content-Type" header
        });
    }

    async getMessages(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_MESSAGES_PATH + "/" + RTMT_GET_URL + "/" + key;
        const response = await fetch(toFetch, {
            method: 'GET', // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            headers: {
              'Content-Type': 'application/json'
            },
            redirect: 'follow', // manual, *follow, error
            referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: null // body data type must match "Content-Type" header
        });
        if (response.status === 200) {
            const data = await response.json();
            return data;
        } else {
            return null;
        }
    }

    messageFilter(messageTextContains, idList, sourceList, severityList) {
        return new MessageFilter(messageTextContains, idList, sourceList, severityList);
    }

}

class EventFilter {
    constructor(parentPath, eventPathList, sourceList, routeList, typeList, severityList, externalIdList) {
        this.parentPath = parentPath;
        this.eventPathList = eventPathList;
        this.sourceList = sourceList;
        this.routeList = routeList;
        this.typeList = typeList;
        this.severityList = severityList;
        this.externalIdList = externalIdList;
    }
}

class ParameterFilter {
    constructor(parentPath, parameterPathList, routeList, validityList, alarmStateList, externalIdList) {
        this.parentPath = parentPath;
        this.parameterPathList = parameterPathList;
        this.routeList = routeList;
        this.validityList = validityList;
        this.alarmStateList = alarmStateList;
        this.externalIdList = externalIdList;
    }
}

class MessageFilter {
    constructor(messageTextContains, idList, sourceList, severityList) {
        this.messageTextContains = messageTextContains;
        this.idList = idList;
        this.sourceList = sourceList;
        this.severityList = severityList;
    }
}