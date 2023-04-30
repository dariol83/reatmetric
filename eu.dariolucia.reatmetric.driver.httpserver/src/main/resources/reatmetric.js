const RTMT_PARAMETERS_PATH = "parameters";
const RTMT_PARAMETER_CURRENT_STATE_PATH = "current";
const RTMT_PARAMETER_STREAM_PATH = "stream";
const RTMT_EVENTS_PATH = "events";
const RTMT_ACTIVITIES_PATH = "activities";
const RTMT_MESSAGES_PATH = "messages";
const RTMT_CONNECTORS_PATH = "connectors";
const RTMT_MODEL_PATH = "model";

const RTMT_REGISTRATION_URL = "register";
const RTMT_GET_URL = "get";
const RTMT_DEREGISTRATION_URL = "deregister";
const RTMT_LIST_URL = "list";
const RTMT_INVOKE_URL = "invoke";
const RTMT_RETRIEVE_URL = "retrieve";

const RTMT_STARTTIME_ARG = "startTime";
const RTMT_ENDTIME_ARG = "endTime";

const RTMT_ENABLE_URL = "enable";
const RTMT_DISABLE_URL = "disable";

class ReatMetric {

    fetchInit(method, body) {
        return {
                method: method, // *GET, POST, PUT, DELETE, etc.
                mode: 'cors', // no-cors, *cors, same-origin
                cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
                headers: {
                  'Content-Type': 'application/json'
                },
                redirect: 'follow', // manual, *follow, error
                referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
                body: body // body data type must match "Content-Type" header
        };
    }

    /*********************************************************
     * Constructor
     *********************************************************/

    constructor(host, port, name) {
        this.host = host;
        this.port = port;
        this.name = name;
    }

    /*********************************************************
     * Model
     *********************************************************/
    async getDescriptor(path) {
        var thePath = path.replaceAll('.', '/')
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_MODEL_PATH + "/" + thePath;
        const response = await fetch(toFetch, this.fetchInit('GET', null));
        const data = await response.json();
        return data;
    }

    async enable(path) {
        var thePath = path.replaceAll('.', '/')
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_MODEL_PATH + "/" + thePath + "/" + RTMT_ENABLE_URL;
        const response = await fetch(toFetch, this.fetchInit('POST', null));
        const data = await response.json();
        return;
    }

    async disable(path) {
        var thePath = path.replaceAll('.', '/')
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_MODEL_PATH + "/" + thePath + "/" + RTMT_DISABLE_URL;
        const response = await fetch(toFetch, this.fetchInit('POST', null));
        const data = await response.json();
        return;
    }

    /*********************************************************
     * Parameters
     *********************************************************/

    async listParameters() {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_PARAMETERS_PATH + "/" + RTMT_LIST_URL;
        const response = await fetch(toFetch, this.fetchInit('GET', null));
        const data = await response.json();
        return data;
    }

    async retrieveParameters(startTime, endTime, filter) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_PARAMETERS_PATH + "/" + RTMT_RETRIEVE_URL + "?" +
        RTMT_STARTTIME_ARG + "=" + startTime + "&" +
        RTMT_ENDTIME_ARG + "=" + endTime;
        const response = await fetch(toFetch, this.fetchInit('POST', JSON.stringify(filter)));
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
        const response = await fetch(toFetch, this.fetchInit('POST', JSON.stringify(filter)));
        const data = await response.json();
        return data.key;
    }

    async deregisterFromStateParameters(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_PARAMETERS_PATH  + "/" + RTMT_PARAMETER_CURRENT_STATE_PATH + "/" + RTMT_DEREGISTRATION_URL + "/" + key;
        const response = await fetch(toFetch, this.fetchInit('DELETE', null));
    }

    async getStateParameters(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_PARAMETERS_PATH + "/" + RTMT_PARAMETER_CURRENT_STATE_PATH  + "/" + RTMT_GET_URL + "/" + key;
        const response = await fetch(toFetch, this.fetchInit('GET', null));
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
        const response = await fetch(toFetch, this.fetchInit('POST', JSON.stringify(filter)));
        const data = await response.json();
        return data.key;
    }

    async deregisterFromStreamParameters(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_PARAMETERS_PATH  + "/" + RTMT_PARAMETER_STREAM_PATH + "/" + RTMT_DEREGISTRATION_URL + "/" + key;
        const response = await fetch(toFetch, this.fetchInit('DELETE', null));
    }

    async getStreamParameters(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_PARAMETERS_PATH + "/" + RTMT_PARAMETER_STREAM_PATH  + "/" + RTMT_GET_URL + "/" + key;
        const response = await fetch(toFetch, this.fetchInit('GET', null));
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
        const response = await fetch(toFetch, this.fetchInit('GET', null));
        const data = await response.json();
        return data;
    }

    async registerToEvents(filter) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_EVENTS_PATH + "/" + RTMT_REGISTRATION_URL;
        const response = await fetch(toFetch, this.fetchInit('POST', JSON.stringify(filter)));
        const data = await response.json();
        return data.key;
    }

    async deregisterFromEvents(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_EVENTS_PATH + "/" + RTMT_DEREGISTRATION_URL + "/" + key;
        const response = await fetch(toFetch, this.fetchInit('DELETE', null));
    }

    async getEvents(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_EVENTS_PATH + "/" + RTMT_GET_URL + "/" + key;
        const response = await fetch(toFetch, this.fetchInit('GET', null));
        if (response.status === 200) {
            const data = await response.json();
            return data;
        } else {
            return null;
        }
    }

    async retrieveEvents(startTime, endTime, filter) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_EVENTS_PATH + "/" + RTMT_RETRIEVE_URL + "?" +
        RTMT_STARTTIME_ARG + "=" + startTime + "&" +
        RTMT_ENDTIME_ARG + "=" + endTime;
        const response = await fetch(toFetch, this.fetchInit('POST', JSON.stringify(filter)));
        const data = await response.json();
        return data;
    }

    eventFilter(parentPath, eventPathList, sourceList, routeList, typeList, severityList, externalIdList) {
        return new EventFilter(parentPath, eventPathList, sourceList, routeList, typeList, severityList, externalIdList);
    }

    /*********************************************************
     * Activities
     *********************************************************/

    async listActivities() {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_ACTIVITIES_PATH + "/" + RTMT_LIST_URL;
        const response = await fetch(toFetch, this.fetchInit('GET', null));
        const data = await response.json();
        return data;
    }

    async registerToActivities(filter) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_ACTIVITIES_PATH + "/" + RTMT_REGISTRATION_URL;
        const response = await fetch(toFetch, this.fetchInit('POST', JSON.stringify(filter)));
        const data = await response.json();
        return data.key;
    }

    async deregisterFromActivities(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_ACTIVITIES_PATH + "/" + RTMT_DEREGISTRATION_URL + "/" + key;
        const response = await fetch(toFetch, this.fetchInit('DELETE', null));
    }

    async getActivities(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_ACTIVITIES_PATH + "/" + RTMT_GET_URL + "/" + key;
        const response = await fetch(toFetch, this.fetchInit('GET', null));
        if (response.status === 200) {
            const data = await response.json();
            return data;
        } else {
            return null;
        }
    }

    async retrieveActivities(startTime, endTime, filter) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_ACTIVITIES_PATH + "/" + RTMT_RETRIEVE_URL + "?" +
        RTMT_STARTTIME_ARG + "=" + startTime + "&" +
        RTMT_ENDTIME_ARG + "=" + endTime;
        const response = await fetch(toFetch, this.fetchInit('POST', JSON.stringify(filter)));
        const data = await response.json();
        return data;
    }

    async invoke(activityRequest) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_ACTIVITIES_PATH + "/" + RTMT_INVOKE_URL;
        const response = await fetch(toFetch, this.fetchInit('POST', JSON.stringify(activityRequest)));
        const data = await response.json();
        return data.id;
    }

    activityFilter(parentPath, activityPathList, sourceList, routeList, typeList, stateList, externalIdList) {
        return new ActivityFilter(parentPath, activityPathList, sourceList, routeList, typeList, stateList, externalIdList);
    }

    activityRequest(activityId, activityPath, activityArguments, activityRoute, activitySource, activityProperties) {
        return new ActivityRequest(activityId, activityPath, activityArguments, activityRoute, activitySource, activityProperties);
    }

    plainArgument(name, value, engineering) {
        return new PlainActivityRequestArgument(name, value, engineering);
    }

    arrayArgument(name, records) {
        return new ArrayActivityRequestArgument(name, records);
    }

    arrayArgumentRecord(elements) {
        return new ArrayActivityRequestArgumentRecord(elements);
    }

    /*********************************************************
     * Operational Messages
     *********************************************************/

    async registerToMessages(filter) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_MESSAGES_PATH + "/" + RTMT_REGISTRATION_URL;
        const response = await fetch(toFetch, this.fetchInit('POST', JSON.stringify(filter)));
        const data = await response.json();
        return data.key;
    }

    async deregisterFromMessages(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_MESSAGES_PATH + "/" + RTMT_DEREGISTRATION_URL + "/" + key;
        const response = await fetch(toFetch, this.fetchInit('DELETE', null));
    }

    async getMessages(key) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_MESSAGES_PATH + "/" + RTMT_GET_URL + "/" + key;
        const response = await fetch(toFetch, this.fetchInit('GET', null));
        if (response.status === 200) {
            const data = await response.json();
            return data;
        } else {
            return null;
        }
    }

    async retrieveMessages(startTime, endTime, filter) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_MESSAGES_PATH + "/" + RTMT_RETRIEVE_URL + "?" +
        RTMT_STARTTIME_ARG + "=" + startTime + "&" +
        RTMT_ENDTIME_ARG + "=" + endTime;
        const response = await fetch(toFetch, this.fetchInit('POST', JSON.stringify(filter)));
        const data = await response.json();
        return data;
    }

    messageFilter(messageTextContains, idList, sourceList, severityList) {
        return new MessageFilter(messageTextContains, idList, sourceList, severityList);
    }

    /*********************************************************
     * Connectors
     *********************************************************/

    async getConnectors() {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_CONNECTORS_PATH;
        const response = await fetch(toFetch, this.fetchInit('GET', null));
        if (response.status === 200) {
            const data = await response.json();
            return data;
        } else {
            return null;
        }
    }

    async getConnector(name) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_CONNECTORS_PATH + "/" + name;
        const response = await fetch(toFetch, this.fetchInit('GET', null));
        if (response.status === 200) {
            const data = await response.json();
            return data;
        } else {
            return null;
        }
    }

    async connect(name) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_CONNECTORS_PATH + "/" + name + "/connect";
        const response = await fetch(toFetch, this.fetchInit('POST', null));
    }

    async disconnect(name) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_CONNECTORS_PATH + "/" + name + "/disconnect";
        const response = await fetch(toFetch, this.fetchInit('POST', null));
    }

    async abort(name) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_CONNECTORS_PATH + "/" + name + "/abort";
        const response = await fetch(toFetch, this.fetchInit('POST', null));
    }

    async getConnectorProperties(name) {
        var toFetch = "http://" + this.host + ":" + this.port + "/" + this.name + "/" + RTMT_CONNECTORS_PATH + "/" + name + "/properties";
        const response = await fetch(toFetch, this.fetchInit('GET', null));
        if (response.status === 200) {
            const data = await response.json();
            return data;
        } else {
            return null;
        }
    }
}

class ActivityFilter {
    constructor(parentPath, activityPathList, sourceList, routeList, typeList, stateList, externalIdList) {
        this.parentPath = parentPath;
        this.activityPathList = activityPathList;
        this.sourceList = sourceList;
        this.routeList = routeList;
        this.typeList = typeList;
        this.stateList = stateList;
        this.externalIdList = externalIdList;
    }
}

class ActivityRequest {
    constructor(activityId, activityPath, activityArguments, activityRoute, activitySource, activityProperties) {
        this.id = activityId;
        this.path = activityPath;
        this.arguments = activityArguments;
        this.route = activityRoute;
        this.source = activitySource;
        this.properties = activityProperties;
    }
}

class PlainActivityRequestArgument {
    // string, object, boolean
    constructor(name, value, engineering) {
        this.name = name;
        this.type = 'plain';
        this.value = value;
        this.engineering = engineering;
    }
}

class ArrayActivityRequestArgument {
    // string, array of records
    constructor(name, records) {
        this.name = name;
        this.type = 'array';
        this.records = records;
    }
}

class ArrayActivityRequestArgumentRecord {
    // string, array of records
    constructor(elements) {
        this.elements = elements;
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