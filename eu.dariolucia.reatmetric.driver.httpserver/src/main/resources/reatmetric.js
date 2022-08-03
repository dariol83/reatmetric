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

    async deregisterToEvents(key) {
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
}