## HTTP Server Driver

The HTTP Server Driver offers a JSON/REST API that can be used by third-party software/web applications, to retrieve monitoring
information related to ReatMetric system.

Examples can be found by running the Reatmetric Test system and the contents present in this folder, to be hosted by a web server.
The IP and port of the test system are hardcoded in the HTML pages.

When the driver is in use, the following API will be available to the specified IP and port.

### Parameter descriptor list

~~~
    GET     http://<host>:<port>/<system name>/parameters/list
~~~

The request returns the list of parameters defined in the system, as an array of objects so defined:

~~~
    {
         "type" : "PARAMETER",
         "path" : <string>,
         "externalId" : <integer>,
         "description" : <string> or null,
         "rawDataType" : <string>, // See ValueTypeEnum enum names in eu.dariolucia.reatmetric.api.value
         "engDataType" : <string>, // See ValueTypeEnum enum names in eu.dariolucia.reatmetric.api.value
         "unit" : <string> or null,
         "synthetic" : <boolean>,
         "settable" : <boolean>
    }
~~~

*Javascript library function*: async listParameters() : return array of parameter descriptors.

### Event descriptor list

~~~
    GET     http://<host>:<port>/<system name>/events/list
~~~

The request returns the list of events defined in the system, as an array of objects so defined:

~~~
    {
         "type" : "EVENT",
         "path" : <string>,
         "externalId" : <integer>,
         "description" : <string> or null,
         "severity" : <string>, // See Severity enum names in eu.dariolucia.reatmetric.api.messages
         "eventType" : <string> or null
    }
~~~

*Javascript library function*: async listEvents() : return array of event descriptors.

### Activity descriptor list

~~~
    GET     http://<host>:<port>/<system name>/activities/list
~~~

The request returns the list of activities defined in the system, as an array of objects so defined:

~~~
    {
         "type" : "ACTIVITY",
         "path" : <string>,
         "externalId" : <integer>,
         "description" : <string> or null,
         "activityType" : <string>, 
         "defaultRoute" : <string>, 
         "arguments" : <array of argument descriptors>,
         "properties" : JSON object // associative array
    }
~~~

An argument descriptor can be plain or array. A plain argument descriptor is defined as:

~~~
    {
         "name" : <string>,
         "description" : <string> or null,
         "type" : "plain", 
         "rawDataType" : <string>, // See ValueTypeEnum enum names in eu.dariolucia.reatmetric.api.value
         "engDataType" : <string>, // See ValueTypeEnum enum names in eu.dariolucia.reatmetric.api.value
         "unit" : <string> or null,
         "fixed" : <boolean>,
         "decalibrationPresent" : <boolean>
    }
~~~

An array argument descriptor is defined as:

~~~
    {
         "name" : <string>,
         "description" : <string> or null,
         "type" : "array", 
         "expansionArgument" : <string>,
         "elements" : <array of argument descriptors>
    }
~~~

*Javascript library function*: async listActivities() : return array of event descriptors.

### Parameter state subscription

This type of interaction allows clients to request the creation of a specific parameter subscription, based on the provided filter. 
On the server, parameters matching the subscription are kept in a map, where the key is the parameter path. Upon each GET, the map values 
are returned to the caller and the map is cleared. Hence the interaction, by design, returns only the latest parameter
state available at the time of each GET, if there were changes with respect to its previously returned state. This type
of interaction can be used to retrieve information to be used in ANDs and mimics. 

**Subscription**
~~~
    POST    http://<host>:<port>/<system name>/parameters/current/register
~~~

The request must provide in its body a ParameterDataFilter in JSON format, according to the following structure:

~~~
    {
         "parentPath" : <string> or null,
         "parameterPathList" : <array of string> or null,
         "routeList" : <array of string> or null,
         "validityList" : <array of string> or null, // See Validity enum names in eu.dariolucia.reatmetric.api.parameters
         "alarmStateList" : <array of string> or null, // See AlarmState enum names in eu.dariolucia.reatmetric.api.model
         "externalIdList" : <array of integer> or null
    }
~~~

The response returns in its body a key value (UUID) in the following structure:

~~~
    {
         "key" : <string> // UUID string
    }
~~~

The effect of this invocation is the creation, on the server side, of a subscription that will deliver the related 
parameters via a GET request (see below).

*Javascript library function*: async registerToStateParameters(filter) : return the key value as string.

*Javascript library function*: parameterFilter(parentPath, parameterPathList, routeList, validityList, alarmStateList, externalIdList) : return a parameter filter object.

**Removal of subscription**
~~~
    DELETE  http://<host>:<port>/<system name>/parameters/current/deregister/<key>
~~~

The effect of this invocation is the removal, on the server side, of the subscription linked to the specified key.

*Javascript library function*: async deregisterFromStateParameters(key) : *void*

**Parameter fetch**
~~~
    GET     http://<host>:<port>/<system name>/parameters/current/get/<key>
~~~

The request returns the list of ParameterData object related to the subscription. Returned objects are removed from the 
server side map. The array of objects so defined:

~~~
    {
        "internalId" : <integer>,
        "gentime" : <time string>, // Format as YYYY-MM-DD'T'hh:mm:ss.SSSZ
        "externalId" : <integer>, 
        "path" : <string>, 
        "eng" : <string or time string or integer or float or boolean> or null,
        "raw" : <string or time string or integer or float or boolean> or null,
        "rcptime" : <time string>, // Format as YYYY-MM-DD'T'hh:mm:ss.SSSZ
        "route" : <string> or null,
        "validity" : <string>, // See Validity enum names in eu.dariolucia.reatmetric.api.parameters
        "alarm" : <string> // See AlarmState enum names in eu.dariolucia.reatmetric.api.model
    }
~~~

*Javascript library function*: async getStateParameters(key) : return array of parameter data objects.

### Parameter stream subscription

This type of interaction allows clients to request the creation of a specific parameter subscription, based on the provided filter.
On the server, each parameter update matching the subscription is kept in a queue. Upon each GET, the values in the queue 
are returned to the caller and the queue is cleared. Hence the interaction, by design, returns all the parameter samples
generated by the system from a previous GET. This type of interaction can be used to retrieve information to be used in 
plots and charts. 

The driver has a maximum queue size per subscription.

**Subscription**
~~~
    POST    http://<host>:<port>/<system name>/parameters/stream/register
~~~

The request must provide in its body a ParameterDataFilter in JSON format, according to the structure defined above.

The response returns in its body a key value according to the structure defined above.

The effect of this invocation is the creation, on the server side, of a subscription that will deliver the related 
parameters via a GET request (see below).

*Javascript library function*: async registerToStreamParameters(filter) : return the key value as string.

**Removal of subscription**
~~~
    DELETE  http://<host>:<port>/<system name>/parameters/stream/deregister/<key>
~~~

The effect of this invocation is the removal, on the server side, of the subscription linked to the specified key.

*Javascript library function*: async deregisterFromStreamParameters(key) : *void*

**Parameter fetch**
~~~
    GET     http://<host>:<port>/<system name>/parameters/stream/get/<key>
~~~

The request returns the list of ParameterData objects related to the subscription. Returned objects are removed from the 
server side queue. The array of objects is defined according to the structure defined above.

*Javascript library function*: async getStreamParameters(key) : return array of parameter data objects.

### Parameter current state

This operation allows to request the current state of a parameter, as currently stored in the processing model.

~~~
    GET     http://<host>:<port>/<system name>/parameters/state?path=<parameter path>
~~~
~~~
    GET     http://<host>:<port>/<system name>/parameters/state?id=<parameter external ID>
~~~

The response returns in its body a single ParameterData object in JSON format (see above "Parameter state subscription").

*Javascript library function*: async getParameterByPath(path) : return a parameter data object.

*Javascript library function*: async getParameterByID(id) : return a parameter data object.

### Event stream subscription

This type of interaction allows clients to request the creation of a specific event subscription, based on the provided filter.
On the server, each event update matching the subscription is kept in a queue. Upon each GET, the values in the queue 
are returned to the caller and the queue is cleared. Hence the interaction, by design, returns all the events
generated by the system from a previous GET. This type of interaction can be used to retrieve information to be used in 
scrollable displays. 

The driver has a maximum queue size per subscription.

**Subscription**
~~~
    POST    http://<host>:<port>/<system name>/events/register
~~~

The request must provide in its body a EventDataFilter in JSON format, according to the following structure.

~~~
    {
         "parentPath" : <string> or null,
         "eventPathList" : <array of string> or null,
         "routeList" : <array of string> or null,
         "typeList" : <array of string> or null, 
         "sourceList" : <array of string> or null,
         "severityList" : <array of string> or null, // See Severity enum names in eu.dariolucia.reatmetric.api.messages
         "externalIdList" : <array of integer> or null
    }
~~~

The response returns in its body a key value according to the structure defined above.

The effect of this invocation is the creation, on the server side, of a subscription that will deliver the related 
events via a GET request (see below).

*Javascript library function*: async registerToEvents(filter) : return the key value as string.

*Javascript library function*: eventFilter(parentPath, eventPathList, sourceList, routeList, typeList, severityList, externalIdList) : return an event filter object.

**Removal of subscription**
~~~
    DELETE  http://<host>:<port>/<system name>/events/deregister/<key>
~~~

The effect of this invocation is the removal, on the server side, of the subscription linked to the specified key.

*Javascript library function*: async deregisterFromEvents(key) : *void*

**Event fetch**
~~~
    GET     http://<host>:<port>/<system name>/events/get/<key>
~~~

The request returns the list of EventData objects related to the subscription. Returned objects are removed from the 
server side queue. The array of objects is so defined.

~~~
    {
        "internalId" : <integer>,
        "gentime" : <time string>, // Format as YYYY-MM-DD'T'hh:mm:ss.SSSZ
        "externalId" : <integer>, 
        "path" : <string>, 
        "qualifier" : <string> or null,
        "rcptime" : <time string>, // Format as YYYY-MM-DD'T'hh:mm:ss.SSSZ
        "type" : <string> or null,
        "route" : <string> or null,
        "source" : <string> or null,
        "severity" : <string> // See Severity enum names in eu.dariolucia.reatmetric.api.messages
    }
~~~

*Javascript library function*: async getEvents(key) : return array of event data objects.

### Activity stream subscription

This type of interaction allows clients to request the creation of a specific activity subscription, based on the provided filter.
On the server, each activity update matching the subscription is kept in a queue. Upon each GET, the values in the queue
are returned to the caller and the queue is cleared. Hence the interaction, by design, returns all the activity occurrence updates
generated by the system from a previous GET. This type of interaction can be used to retrieve information to be used in
scrollable displays.

The driver has a maximum queue size per subscription.

**Subscription**
~~~
    POST    http://<host>:<port>/<system name>/activities/register
~~~

The request must provide in its body a ActivityOccurrenceDataFilter in JSON format, according to the following structure.

~~~
    {
         "parentPath" : <string> or null,
         "activityPathList" : <array of string> or null,
         "routeList" : <array of string> or null,
         "typeList" : <array of string> or null, 
         "sourceList" : <array of string> or null,
         "stateList" : <array of string> or null, // See ActivityOccurrenceState enum names in eu.dariolucia.reatmetric.api.activity
         "externalIdList" : <array of integer> or null
    }
~~~

The response returns in its body a key value according to the structure defined above.

The effect of this invocation is the creation, on the server side, of a subscription that will deliver the related
activity occurrence updates via a GET request (see below).

*Javascript library function*: async registerToActivities(filter) : return the key value as string.

*Javascript library function*: activityFilter(parentPath, activityPathList, sourceList, routeList, typeList, stateList, externalIdList) : return an activity occurrence filter object.

**Removal of subscription**
~~~
    DELETE  http://<host>:<port>/<system name>/activities/deregister/<key>
~~~

The effect of this invocation is the removal, on the server side, of the subscription linked to the specified key.

*Javascript library function*: async deregisterFromActivities(key) : *void*

**Activity Occurrence fetch**
~~~
    GET     http://<host>:<port>/<system name>/activities/get/<key>
~~~

The request returns the list of ActivityOccurrenceData objects related to the subscription. Returned objects are removed from the
server side queue. The array of objects is so defined.

~~~
    {
        "internalId" : <integer>,
        "gentime" : <time string>, // Format as YYYY-MM-DD'T'hh:mm:ss.SSSZ
        "externalId" : <integer>, 
        "path" : <string>, 
        "name" : <string>, 
        "exectime" : <time string>, // Format as YYYY-MM-DD'T'hh:mm:ss.SSSZ
        "type" : <string> or null,
        "route" : <string> or null,
        "source" : <string> or null,
        "currentState" : <string> // See ActivityOccurrenceState enum names in eu.dariolucia.reatmetric.api.activity
        "result" : <string or time string or integer or float or boolean> or null,
        "arguments" : <JSON object with key-value (any value) pairs>, 
        "properties" : <JSON object with key-value (string) pairs>,
        "reports" : <array of reports>: 
        {
            "internalId" : <integer>,
            "gentime" : <time string>, // Format as YYYY-MM-DD'T'hh:mm:ss.SSSZ
            "name" : <string>,
            "exectime" : <time string>, // Format as YYYY-MM-DD'T'hh:mm:ss.SSSZ
            "state" : <string> // See ActivityOccurrenceState enum names in eu.dariolucia.reatmetric.api.activity
            "transition" : <string> // See ActivityOccurrenceState enum names in eu.dariolucia.reatmetric.api.activity
            "status" : <string> // See ActivityReportState enum names in eu.dariolucia.reatmetric.api.activity
            "result" : <string or time string or integer or float or boolean> or null
        }        
    }
   
~~~

*Javascript library function*: async getActivities(key) : return array of activity occurrence data objects.

### Activity invocation

This type of interaction allows clients to request the execution of a specific activity, based on the provided request.

**Invocation**
~~~
    POST    http://<host>:<port>/<system name>/activities/invoke
~~~

The request must provide in its body an ActivityRequest in JSON format, according to the following structure.

~~~
    {
         "id" : <integer>,
         "path" : <string>,
         "route" : <string>,
         "arguments" : <array of activity arguments>, 
         "source" : <string>,
         "properties" : <JSON object with key-value (string) pairs>
    }
~~~

The array of activity arguments contains either plain or array activity arguments.

~~~
    Plain:
    {
         "name" : <string>,
         "type" : "plain",
         "value" : <string or time string or integer or float or boolean> or null,
         "engineering" : <boolean> 
    }
    
    Array:
    {
         "name" : <string>,
         "type" : "array",
         "records" : <array of records>:
         {
            "elements" : <array of activity arguments>
         }
    }
~~~

The response returns in its body an id value according to the structure defined below.

~~~
    {
         "id" : <integer> // Activity occurrence ID
    }
~~~

*Javascript library function*: async invoke(activityRequest) : return the id as integer.

*Javascript library function*: activityRequest(activityId, activityPath, activityArguments, activityRoute, activitySource, activityProperties) : return an activity request object.

*Javascript library function*: plainArgument(name, value, engineering) : return an activity request plain argument.

*Javascript library function*: arrayArgument(name, records) : return an activity request array argument.

*Javascript library function*: arrayArgumentRecord(elements) : return an activity request array argument record.


### Operational message stream subscription

This type of interaction allows clients to request the creation of a specific operational message subscription, based on the provided filter.
On the server, each operational message matching the subscription is kept in a queue. Upon each GET, the values in the queue 
are returned to the caller and the queue is cleared. Hence the interaction, by design, returns all the messages
generated by the system from a previous GET. This type of interaction can be used to retrieve information to be used in 
scrollable displays. 

The driver has a maximum queue size per subscription.

**Subscription**
~~~
    POST    http://<host>:<port>/<system name>/messages/register
~~~

The request must provide in its body a OperationalMessageDataFilter in JSON format, according to the following structure.

~~~
    {
         "messageTextContains" : <string> or null,
         "idList" : <array of string> or null,
         "sourceList" : <array of string> or null,
         "severityList" : <array of string> or null // See Severity enum names in eu.dariolucia.reatmetric.api.messages
    }
~~~

The response returns in its body a key value according to the structure defined above.

The effect of this invocation is the creation, on the server side, of a subscription that will deliver the related 
messages via a GET request (see below).

*Javascript library function*: async registerToMessages(filter) : return the key value as string.

*Javascript library function*: messageFilter(messageTextContains, idList, sourceList, severityList) : return an operational message filter object.

**Removal of subscription**
~~~
    DELETE  http://<host>:<port>/<system name>/messages/deregister/<key>
~~~

The effect of this invocation is the removal, on the server side, of the subscription linked to the specified key.

*Javascript library function*: async deregisterFromMessages(key) : *void*

**Operational messages fetch**
~~~
    GET     http://<host>:<port>/<system name>/messages/get/<key>
~~~

The request returns the list of OperationalMessageData objects related to the subscription. Returned objects are removed from the 
server side queue. The array of objects is so defined.

~~~
    {
        "internalId" : <integer>,
        "gentime" : <time string>, // Format as YYYY-MM-DD'T'hh:mm:ss.SSSZ
        "id" : <string> or null,
        "message" : <string>, 
        "source" : <string> or null,
        "severity" : <string> // See Severity enum names in eu.dariolucia.reatmetric.api.messages
    }
~~~

*Javascript library function*: async getMessages(key) : return array of operational message data objects.

### Model entity descriptor fetch

~~~
    GET     http://<host>:<port>/<system name>/model/<path of system element, with / separator instead of . separator>
~~~

If successful, the request returns an object with two properties:
- element: it contains the descriptor of the entity referenced by the <path of system element>. If the path is not provided, then null is returned
- children: it contains an array with the descriptors of the child elements. If the element has no children, an empty array is returned.

~~~
    {
        "element" : <descriptor>,
        "children" : [
            <descriptor>,
            <descriptor>,
            <descriptor>,
            ...
            <descriptor>
        ]
    }
~~~

The descriptors have the following structure.

**Container**
~~~
    {
        "type" : "CONTAINER", 
        "path" : <string> // full system entity path with . separator  
    }
~~~

**Parameter**

Defined as above ("Parameter descriptor list").

**Event**

Defined as above ("Event descriptor list"). 

**Activity**

Defined as above ("Activity descriptor list").

*Javascript library function*: async getDescriptor(path) : return the element-children information assigned to the provided path.

### Model entity enable/disable

~~~
    POST     http://<host>:<port>/<system name>/model/<path of system element, with / separator instead of . separator>/[enable|disable]
~~~

This request enables or disables the processing linked to the specified system element.

*Javascript library function*: async enable(path) : *void*

*Javascript library function*: async disable(path) : *void*

### Historical data retrieval

These operations can be used to retrieve archived parameter data, event data, activity occurrences and messages.

**Parameter**
~~~
    POST    http://<host>:<port>/<system name>/parameters/retrieve?startTime=<time in ms from UNIX epoch>&endTime=<time in ms from UNIX epoch>
~~~

The body of the request is a ParameterDataFilter in JSON format (see "Parameter state subscription").
The request returns an array of ParameterData in JSON format (see "Parameter state subscription").

*Javascript library function*: async retrieveParameters(startTime, endTime, filter) : return array of ParameterData objects.

**Event**
~~~
    POST    http://<host>:<port>/<system name>/events/retrieve?startTime=<time in ms from UNIX epoch>&endTime=<time in ms from UNIX epoch>
~~~

The body of the request is a EventDataFilter in JSON format (see "Event stream subscription").
The request returns an array of EventData in JSON format (see "Event stream subscription").

*Javascript library function*: async retrieveEvents(startTime, endTime, filter) : return array of EventData objects.

**Activities**
~~~
    POST    http://<host>:<port>/<system name>/activities/retrieve?startTime=<time in ms from UNIX epoch>&endTime=<time in ms from UNIX epoch>
~~~

The body of the request is a ActivityOccurrenceDataFilter in JSON format (see "Activity stream subscription").
The request returns an array of ActivityOccurrenceData in JSON format (see "Activity stream subscription").

*Javascript library function*: async retrieveActivities(startTime, endTime, filter) : return array of ActivityOccurrenceData objects.

**Messages**
~~~
    POST    http://<host>:<port>/<system name>/messages/retrieve?startTime=<time in ms from UNIX epoch>&endTime=<time in ms from UNIX epoch>
~~~

The body of the request is a OperationalMessageDataFilter in JSON format (see "Message stream subscription").
The request returns an array of OperationalMessageData in JSON format (see "Message stream subscription").

*Javascript library function*: async retrieveMessages(startTime, endTime, filter) : return array of OperationalMessageData objects.

### Connectors

**Connector list fetch**

~~~
    GET     http://<host>:<port>/<system name>/connectors
~~~

The request returns the list of connectors defined in the system, as an array of objects so defined:

~~~
    {
        "name" : <string>,
        "description" : <string>,
        "alarmState" : <string>, // See AlarmState enum names
        "status" : <string>, // See TransportConnectionStatus enum names
        "rx" : <integer>, // RX data rate
        "tx" : <integer>, // TX data rate
        "autoreconnect" : <boolean> // true or false
    }
~~~

*Javascript library function*: async getConnectors() : return a list of connector objects

**Connector fetch**

~~~
    GET     http://<host>:<port>/<system name>/connectors/<connector name>
~~~

The request returns the connector status, using the same format defined for the connector list.

*Javascript library function*: async getConnector(name) : return the connector object

~~~
    GET     http://<host>:<port>/<system name>/connectors/<connector name>/properties
~~~

The request returns the properties defined by the connector, as a JSON object (key-value map).

*Javascript library function*: async getConnectorProperties(name) : return the connector properties

**Connector operations**

~~~
    POST     http://<host>:<port>/<system name>/connectors/<connector name/connect
~~~
The request connects the connector. No body defined.

*Javascript library function*: async connect(name) : *void*

~~~
    POST     http://<host>:<port>/<system name>/connectors/<connector name/disconnect
~~~
The request disconnects the connector. No body defined.

*Javascript library function*: async disconnect(name) : *void*

~~~
    POST     http://<host>:<port>/<system name>/connectors/<connector name/abort
~~~
The request aborts the connector. No body defined.

*Javascript library function*: async abort(name) : *void*

~~~
    POST     http://<host>:<port>/<system name>/connectors/<connector name/reconnect
~~~
The request sets the reconnection flag of the connector. The body is:

~~~
    {
        "input" : <boolean> // true or false
    }
~~~

~~~
    POST     http://<host>:<port>/<system name>/connectors/<connector name/initialise
~~~
The request initialises the connector with the provided set of properties. The body is a JSON object (key-value map). 


